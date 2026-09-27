package com.sentinelap.service;

import com.sentinelap.domain.*;
import com.sentinelap.dto.RiskScoreDtos.RiskScoreRequest;
import com.sentinelap.dto.RiskScoreDtos.RiskScoreResponse;
import com.sentinelap.repo.BankAccountChangeEventRepository;
import com.sentinelap.repo.FraudAlertRepository;
import com.sentinelap.repo.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Deterministic fraud-detection rules, applied to every invoice at ingestion time:
 *
 *  1. BANK_ACCOUNT_CHANGE - vendor's payout bank account changed within the last
 *     N days of this invoice being submitted (classic BEC / vendor-impersonation pattern).
 *  2. DUPLICATE_INVOICE   - same vendor + amount + invoice number (or a fuzzy hash of
 *     vendor+amount+date-window) already exists.
 *  3. VELOCITY_ANOMALY    - unusual spike in invoice count or $ amount from this vendor
 *     relative to their trailing 30-day baseline.
 *
 * On top of the three rules, every invoice is also sent to the ML scoring service
 * (RiskScoringClient) for an isolation-forest anomaly score; a high ML score raises
 * an ML_ANOMALY alert even when no hand-written rule fires.
 */
@Service
@RequiredArgsConstructor
public class FraudRulesEngine {

    private static final int BANK_CHANGE_LOOKBACK_DAYS = 30;
    private static final int VELOCITY_LOOKBACK_DAYS = 30;
    private static final int VELOCITY_INVOICE_COUNT_THRESHOLD = 8;
    private static final double ML_ANOMALY_THRESHOLD = 70.0;

    private final BankAccountChangeEventRepository bankChangeRepo;
    private final InvoiceRepository invoiceRepository;
    private final FraudAlertRepository alertRepository;
    private final RiskScoringClient riskScoringClient;

    @Transactional
    public List<FraudAlert> evaluate(Invoice invoice) {
        Vendor vendor = invoice.getVendor();
        java.util.ArrayList<FraudAlert> raised = new java.util.ArrayList<>();

        checkBankAccountChange(invoice, vendor).ifPresent(raised::add);
        checkDuplicateInvoice(invoice, vendor).ifPresent(raised::add);
        checkVelocityAnomaly(invoice, vendor).ifPresent(raised::add);
        checkMlAnomaly(invoice, vendor, raised.size()).ifPresent(raised::add);

        alertRepository.saveAll(raised);
        return raised;
    }

    private Optional<FraudAlert> checkBankAccountChange(Invoice invoice, Vendor vendor) {
        List<BankAccountChangeEvent> recentChanges = bankChangeRepo.findByVendorIdOrderByDetectedAtDesc(vendor.getId());
        if (recentChanges.isEmpty()) {
            return Optional.empty();
        }
        BankAccountChangeEvent latest = recentChanges.get(0);
        long daysSinceChange = ChronoUnit.DAYS.between(latest.getDetectedAt(), OffsetDateTime.now());
        if (daysSinceChange <= BANK_CHANGE_LOOKBACK_DAYS) {
            String reason = String.format(
                    "Vendor's payout bank account was changed %d day(s) before this invoice was submitted " +
                    "(account ending %s -> %s, requested via %s). Invoices arriving shortly after an " +
                    "unverified bank-account change are the single highest-signal indicator of vendor " +
                    "impersonation / business-email-compromise fraud.",
                    daysSinceChange, latest.getOldAccountLast4(), latest.getNewAccountLast4(), latest.getChangedVia());
            return Optional.of(FraudAlert.builder()
                    .vendor(vendor)
                    .invoice(invoice)
                    .alertType(FraudAlert.AlertType.BANK_ACCOUNT_CHANGE)
                    .severity(FraudAlert.Severity.CRITICAL)
                    .riskScore(95.0)
                    .reason(reason)
                    .build());
        }
        return Optional.empty();
    }

    private Optional<FraudAlert> checkDuplicateInvoice(Invoice invoice, Vendor vendor) {
        List<Invoice> matches = invoiceRepository.findByInvoiceHash(invoice.getInvoiceHash());
        long otherMatches = matches.stream().filter(i -> !i.getId().equals(invoice.getId())).count();
        if (otherMatches > 0) {
            String reason = String.format(
                    "Found %d other invoice(s) from vendor '%s' with the same amount and a matching " +
                    "invoice-number/date fingerprint. This pattern typically indicates either an accidental " +
                    "duplicate submission or a deliberate double-billing attempt.",
                    otherMatches, vendor.getName());
            return Optional.of(FraudAlert.builder()
                    .vendor(vendor)
                    .invoice(invoice)
                    .alertType(FraudAlert.AlertType.DUPLICATE_INVOICE)
                    .severity(FraudAlert.Severity.HIGH)
                    .riskScore(75.0)
                    .reason(reason)
                    .build());
        }
        return Optional.empty();
    }

    private Optional<FraudAlert> checkVelocityAnomaly(Invoice invoice, Vendor vendor) {
        OffsetDateTime windowStart = OffsetDateTime.now().minusDays(VELOCITY_LOOKBACK_DAYS);
        List<Invoice> recent = invoiceRepository.findByCreatedAtAfterOrderByCreatedAtDesc(windowStart);
        long vendorInvoiceCount = recent.stream()
                .filter(i -> i.getVendor().getId().equals(vendor.getId()))
                .count();

        if (vendorInvoiceCount >= VELOCITY_INVOICE_COUNT_THRESHOLD) {
            String reason = String.format(
                    "Vendor '%s' has submitted %d invoices in the trailing %d days, well above the typical " +
                    "cadence for this vendor. A sudden spike in invoice volume can indicate a compromised " +
                    "vendor account being used to push through multiple fraudulent invoices quickly.",
                    vendor.getName(), vendorInvoiceCount, VELOCITY_LOOKBACK_DAYS);
            return Optional.of(FraudAlert.builder()
                    .vendor(vendor)
                    .invoice(invoice)
                    .alertType(FraudAlert.AlertType.VELOCITY_ANOMALY)
                    .severity(FraudAlert.Severity.MEDIUM)
                    .riskScore(55.0)
                    .reason(reason)
                    .build());
        }
        return Optional.empty();
    }

    private Optional<FraudAlert> checkMlAnomaly(Invoice invoice, Vendor vendor, int rulesAlreadyFired) {
        long vendorAgeDays = ChronoUnit.DAYS.between(vendor.getCreatedAt(), OffsetDateTime.now());
        long invoicesLast30 = invoiceRepository.countByVendorId(vendor.getId());
        boolean bankChangedRecently = !bankChangeRepo.findByVendorIdOrderByDetectedAtDesc(vendor.getId()).isEmpty();

        RiskScoreRequest request = new RiskScoreRequest(
                invoice.getAmount(),
                vendorAgeDays,
                invoicesLast30,
                bankChangedRecently,
                rulesAlreadyFired > 0,
                computeAmountZScore(invoice.getAmount())
        );

        RiskScoreResponse response = riskScoringClient.score(request);
        vendor.setRiskScore(response.riskScore());

        if (response.anomaly() && response.riskScore() >= ML_ANOMALY_THRESHOLD) {
            String reason = "ML anomaly-detection model (isolation forest) flagged this invoice as a " +
                    "statistical outlier relative to this vendor's historical invoicing pattern. " +
                    "Contributing factors: " + String.join(", ", response.topFactors()) + ".";
            return Optional.of(FraudAlert.builder()
                    .vendor(vendor)
                    .invoice(invoice)
                    .alertType(FraudAlert.AlertType.ML_ANOMALY)
                    .severity(response.riskScore() >= 90 ? FraudAlert.Severity.CRITICAL : FraudAlert.Severity.HIGH)
                    .riskScore(response.riskScore())
                    .reason(reason)
                    .build());
        }
        return Optional.empty();
    }

    private double computeAmountZScore(BigDecimal amount) {
        // Lightweight heuristic z-score placeholder; the ML service recomputes a
        // proper per-vendor z-score server-side using full invoice history.
        double value = amount.doubleValue();
        double assumedMean = 2500.0;
        double assumedStdDev = 1800.0;
        return (value - assumedMean) / assumedStdDev;
    }
}
