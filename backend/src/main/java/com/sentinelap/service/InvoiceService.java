package com.sentinelap.service;

import com.sentinelap.domain.FraudAlert;
import com.sentinelap.domain.Invoice;
import com.sentinelap.dto.InvoiceDto;
import com.sentinelap.repo.FraudAlertRepository;
import com.sentinelap.repo.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final FraudRulesEngine fraudRulesEngine;

    public boolean existsByExternalId(String externalId) {
        return invoiceRepository.findByExternalId(externalId).isPresent();
    }

    public List<InvoiceDto> listInvoices() {
        Set<UUID> flaggedInvoiceIds = fraudAlertRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(a -> a.getInvoice() != null ? a.getInvoice().getId() : null)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());

        return invoiceRepository.findAll().stream()
                .map(inv -> toDto(inv, flaggedInvoiceIds.contains(inv.getId())))
                .sorted((a, b) -> b.issuedDate().compareTo(a.issuedDate()))
                .toList();
    }

    @Transactional
    public List<FraudAlert> ingestAndEvaluate(Invoice invoice) {
        invoice.setInvoiceHash(computeHash(invoice));
        Invoice saved = invoiceRepository.save(invoice);
        List<FraudAlert> alerts = fraudRulesEngine.evaluate(saved);
        if (!alerts.isEmpty()) {
            saved.setStatus(Invoice.InvoiceStatus.FLAGGED);
            invoiceRepository.save(saved);
        }
        return alerts;
    }

    public static String computeHash(Invoice invoice) {
        try {
            String raw = invoice.getVendor().getId() + "|" + invoice.getAmount() + "|" + invoice.getInvoiceNumber();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private InvoiceDto toDto(Invoice inv, boolean flagged) {
        return new InvoiceDto(
                inv.getId(), inv.getVendor().getId(), inv.getVendor().getName(),
                inv.getInvoiceNumber(), inv.getAmount(), inv.getCurrency(),
                inv.getIssuedDate(), inv.getDueDate(), inv.getStatus().name(), flagged
        );
    }
}
