package com.sentinelap.integration.quickbooks;

import com.fasterxml.jackson.databind.JsonNode;
import com.sentinelap.domain.Invoice;
import com.sentinelap.domain.QuickBooksConnection;
import com.sentinelap.domain.Vendor;
import com.sentinelap.repo.QuickBooksConnectionRepository;
import com.sentinelap.repo.VendorRepository;
import com.sentinelap.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Pulls Vendor and Bill records out of a connected QuickBooks Online (sandbox or
 * production) company and maps them into SentinelAP's own Vendor/Invoice model so
 * the same fraud-rules engine and ML scoring runs against real accounting data,
 * not just the synthetic seed set.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuickBooksSyncService {

    private final QuickBooksClient quickBooksClient;
    private final QuickBooksConnectionRepository connectionRepository;
    private final VendorRepository vendorRepository;
    private final InvoiceService invoiceService;

    @Transactional
    public SyncResult syncAll(QuickBooksConnection connection) {
        if (OffsetDateTime.now().isAfter(connection.getAccessTokenExpiresAt().minusMinutes(2))) {
            connection = quickBooksClient.refreshTokens(connection);
            connectionRepository.save(connection);
        }

        int vendorsSynced = syncVendors(connection);
        int invoicesSynced = syncBills(connection);

        connection.setLastSyncedAt(OffsetDateTime.now());
        connectionRepository.save(connection);

        return new SyncResult(vendorsSynced, invoicesSynced);
    }

    private int syncVendors(QuickBooksConnection connection) {
        JsonNode response = quickBooksClient.query(connection, "select * from Vendor maxresults 200");
        JsonNode vendorNodes = response.path("QueryResponse").path("Vendor");
        int count = 0;
        for (JsonNode v : vendorNodes) {
            String externalId = v.path("Id").asText();
            String name = v.path("DisplayName").asText();
            Vendor vendor = vendorRepository.findByExternalId(externalId).orElseGet(() ->
                    Vendor.builder().externalId(externalId).source(Vendor.DataSource.QUICKBOOKS).build());
            vendor.setName(name);
            vendor.setEmail(v.path("PrimaryEmailAddr").path("Address").asText(null));
            vendor.setPhone(v.path("PrimaryPhone").path("FreeFormNumber").asText(null));
            vendor.setUpdatedAt(OffsetDateTime.now());
            vendorRepository.save(vendor);
            count++;
        }
        log.info("QuickBooks sync: {} vendors upserted", count);
        return count;
    }

    private int syncBills(QuickBooksConnection connection) {
        JsonNode response = quickBooksClient.query(connection, "select * from Bill maxresults 200");
        JsonNode billNodes = response.path("QueryResponse").path("Bill");
        int count = 0;
        for (JsonNode b : billNodes) {
            String externalId = b.path("Id").asText();
            if (invoiceService.existsByExternalId(externalId)) {
                continue; // already imported; QuickBooks bills don't change after posting in this simplified sync
            }
            String vendorExternalId = b.path("VendorRef").path("value").asText();
            Vendor vendor = vendorRepository.findByExternalId(vendorExternalId).orElse(null);
            if (vendor == null) {
                continue;
            }
            BigDecimal amount = new BigDecimal(b.path("TotalAmt").asText("0"));
            LocalDate txnDate = LocalDate.parse(b.path("TxnDate").asText(LocalDate.now().toString()));
            LocalDate dueDate = b.path("DueDate").isMissingNode() ? txnDate.plusDays(30)
                    : LocalDate.parse(b.path("DueDate").asText());

            Invoice invoice = Invoice.builder()
                    .vendor(vendor)
                    .invoiceNumber(b.path("DocNumber").asText("QBO-" + externalId))
                    .amount(amount)
                    .currency("USD")
                    .issuedDate(txnDate)
                    .dueDate(dueDate)
                    .status(Invoice.InvoiceStatus.PENDING)
                    .source(Vendor.DataSource.QUICKBOOKS)
                    .externalId(externalId)
                    .build();
            invoiceService.ingestAndEvaluate(invoice);
            count++;
        }
        log.info("QuickBooks sync: {} bills imported and evaluated for fraud", count);
        return count;
    }

    public record SyncResult(int vendorsSynced, int invoicesSynced) {}
}
