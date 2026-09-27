package com.sentinelap.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AlertDto(
        UUID id,
        UUID vendorId,
        String vendorName,
        UUID invoiceId,
        String invoiceNumber,
        String alertType,
        String severity,
        Double riskScore,
        String reason,
        String status,
        OffsetDateTime createdAt
) {}
