package com.sentinelap;

import com.sentinelap.domain.Invoice;
import com.sentinelap.domain.Vendor;
import com.sentinelap.service.InvoiceService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class InvoiceHashTest {

    @Test
    void identicalVendorAmountAndNumberProduceSameHash() {
        Vendor vendor = Vendor.builder().id(UUID.randomUUID()).name("Acme Co").build();

        Invoice a = Invoice.builder().vendor(vendor).amount(new BigDecimal("100.00"))
                .invoiceNumber("INV-1").issuedDate(LocalDate.now()).build();
        Invoice b = Invoice.builder().vendor(vendor).amount(new BigDecimal("100.00"))
                .invoiceNumber("INV-1").issuedDate(LocalDate.now().minusDays(5)).build();

        assertEquals(InvoiceService.computeHash(a), InvoiceService.computeHash(b));
    }

    @Test
    void differentAmountProducesDifferentHash() {
        Vendor vendor = Vendor.builder().id(UUID.randomUUID()).name("Acme Co").build();

        Invoice a = Invoice.builder().vendor(vendor).amount(new BigDecimal("100.00"))
                .invoiceNumber("INV-1").issuedDate(LocalDate.now()).build();
        Invoice b = Invoice.builder().vendor(vendor).amount(new BigDecimal("999.00"))
                .invoiceNumber("INV-1").issuedDate(LocalDate.now()).build();

        assertNotEquals(InvoiceService.computeHash(a), InvoiceService.computeHash(b));
    }
}
