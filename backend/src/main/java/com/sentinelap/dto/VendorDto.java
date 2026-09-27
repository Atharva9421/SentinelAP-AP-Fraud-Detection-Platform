package com.sentinelap.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record VendorDto(
        UUID id,
        String name,
        String taxId,
        String email,
        String phone,
        String bankAccountLast4,
        String bankRoutingNumber,
        OffsetDateTime bankAccountUpdatedAt,
        Double riskScore,
        Boolean isVerified,
        String source,
        long openAlertCount,
        long invoiceCount
) {}
