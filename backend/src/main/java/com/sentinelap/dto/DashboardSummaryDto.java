package com.sentinelap.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record DashboardSummaryDto(
        long totalVendors,
        long totalInvoices,
        BigDecimal totalApExposure,
        long openAlerts,
        long criticalAlerts,
        double averageRiskScore,
        Map<String, Long> alertsByType,
        List<AlertDto> recentAlerts
) {}
