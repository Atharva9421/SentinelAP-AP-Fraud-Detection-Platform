package com.sentinelap.repo;

import com.sentinelap.domain.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    List<Invoice> findByVendorIdOrderByIssuedDateDesc(UUID vendorId);

    List<Invoice> findByInvoiceHash(String invoiceHash);

    Optional<Invoice> findByExternalId(String externalId);

    List<Invoice> findByCreatedAtAfterOrderByCreatedAtDesc(OffsetDateTime after);

    long countByVendorId(UUID vendorId);
}
