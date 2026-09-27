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
@Table(name = "quickbooks_connections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuickBooksConnection {

    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private String realmId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String accessToken;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String refreshToken;

    @Column(nullable = false)
    private OffsetDateTime accessTokenExpiresAt;

    @Column(nullable = false)
    private OffsetDateTime refreshTokenExpiresAt;

    @Column(nullable = false)
    @Builder.Default
    private OffsetDateTime connectedAt = OffsetDateTime.now();

    private OffsetDateTime lastSyncedAt;
}
