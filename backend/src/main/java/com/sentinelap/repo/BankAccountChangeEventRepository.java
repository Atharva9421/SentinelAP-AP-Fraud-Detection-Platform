package com.sentinelap.repo;

import com.sentinelap.domain.BankAccountChangeEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BankAccountChangeEventRepository extends JpaRepository<BankAccountChangeEvent, UUID> {
    List<BankAccountChangeEvent> findByVendorIdOrderByDetectedAtDesc(UUID vendorId);
}
