package com.sentinelap.repo;

import com.sentinelap.domain.QuickBooksConnection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuickBooksConnectionRepository extends JpaRepository<QuickBooksConnection, UUID> {
    Optional<QuickBooksConnection> findByRealmId(String realmId);
    Optional<QuickBooksConnection> findFirstByOrderByConnectedAtDesc();
    List<QuickBooksConnection> findAll();
}
