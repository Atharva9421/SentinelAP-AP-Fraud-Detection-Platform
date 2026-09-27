package com.sentinelap.repo;

import com.sentinelap.domain.FraudAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FraudAlertRepository extends JpaRepository<FraudAlert, UUID> {
    List<FraudAlert> findByStatusOrderByCreatedAtDesc(FraudAlert.AlertStatus status);
    List<FraudAlert> findAllByOrderByCreatedAtDesc();
    List<FraudAlert> findByVendorIdOrderByCreatedAtDesc(UUID vendorId);
    long countByStatus(FraudAlert.AlertStatus status);
}
