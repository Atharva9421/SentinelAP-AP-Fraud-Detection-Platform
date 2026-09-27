package com.sentinelap.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InvoiceDto(
        UUID id,
        UUID vendorId,
        String vendorName,
        String invoiceNumber,
        BigDecimal amount,
        String currency,
        LocalDate issuedDate,
        LocalDate dueDate,
        String status,
        boolean flagged
) {}
