package com.sentinelap.service;

import com.sentinelap.domain.FraudAlert;
import com.sentinelap.dto.AlertDto;
import com.sentinelap.repo.FraudAlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final FraudAlertRepository fraudAlertRepository;

    public List<AlertDto> listAlerts(String statusFilter) {
        List<FraudAlert> alerts = (statusFilter == null || statusFilter.isBlank())
                ? fraudAlertRepository.findAllByOrderByCreatedAtDesc()
                : fraudAlertRepository.findByStatusOrderByCreatedAtDesc(FraudAlert.AlertStatus.valueOf(statusFilter.toUpperCase()));
        return alerts.stream().map(this::toDto).toList();
    }

    @Transactional
    public AlertDto updateStatus(UUID alertId, String newStatus, String resolvedBy) {
        FraudAlert alert = fraudAlertRepository.findById(alertId)
                .orElseThrow(() -> new NoSuchElementException("Alert not found: " + alertId));
        alert.setStatus(FraudAlert.AlertStatus.valueOf(newStatus.toUpperCase()));
        if (alert.getStatus() != FraudAlert.AlertStatus.OPEN && alert.getStatus() != FraudAlert.AlertStatus.REVIEWING) {
            alert.setResolvedAt(OffsetDateTime.now());
            alert.setResolvedBy(resolvedBy);
        }
        return toDto(fraudAlertRepository.save(alert));
    }

    private AlertDto toDto(FraudAlert a) {
        return new AlertDto(
                a.getId(), a.getVendor().getId(), a.getVendor().getName(),
                a.getInvoice() != null ? a.getInvoice().getId() : null,
                a.getInvoice() != null ? a.getInvoice().getInvoiceNumber() : null,
                a.getAlertType().name(), a.getSeverity().name(), a.getRiskScore(),
                a.getReason(), a.getStatus().name(), a.getCreatedAt()
        );
    }
}
