package com.sentinelap.service;

import com.sentinelap.domain.FraudAlert;
import com.sentinelap.domain.Invoice;
import com.sentinelap.dto.AlertDto;
import com.sentinelap.dto.DashboardSummaryDto;
import com.sentinelap.repo.FraudAlertRepository;
import com.sentinelap.repo.InvoiceRepository;
import com.sentinelap.repo.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final VendorRepository vendorRepository;
    private final InvoiceRepository invoiceRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final AlertService alertService;

    @Transactional(readOnly = true)
    public DashboardSummaryDto summary() {
        long totalVendors = vendorRepository.count();
        List<Invoice> allInvoices = invoiceRepository.findAll();
        long totalInvoices = allInvoices.size();
        BigDecimal totalExposure = allInvoices.stream()
                .map(Invoice::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long openAlerts = fraudAlertRepository.countByStatus(FraudAlert.AlertStatus.OPEN);
        List<FraudAlert> all = fraudAlertRepository.findAllByOrderByCreatedAtDesc();
        long criticalAlerts = all.stream().filter(a -> a.getSeverity() == FraudAlert.Severity.CRITICAL).count();

        double avgRisk = vendorRepository.findAll().stream()
                .mapToDouble(v -> v.getRiskScore() == null ? 0.0 : v.getRiskScore())
                .average().orElse(0.0);

        Map<String, Long> alertsByType = all.stream()
                .collect(Collectors.groupingBy(a -> a.getAlertType().name(), Collectors.counting()));

        List<AlertDto> recent = alertService.listAlerts(null).stream().limit(10).toList();

        return new DashboardSummaryDto(totalVendors, totalInvoices, totalExposure, openAlerts,
                criticalAlerts, avgRisk, alertsByType, recent);
    }
}
