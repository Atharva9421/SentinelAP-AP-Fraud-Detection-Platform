package com.sentinelap.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "bank_account_change_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankAccountChangeEvent {

    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    private String oldAccountLast4;
    private String oldRoutingNumber;
    private String newAccountLast4;
    private String newRoutingNumber;

    @Column(nullable = false)
    @Builder.Default
    private String changedVia = "EMAIL_REQUEST";

    @Column(nullable = false)
    @Builder.Default
    private OffsetDateTime detectedAt = OffsetDateTime.now();
}
