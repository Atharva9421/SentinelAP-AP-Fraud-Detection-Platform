package com.sentinelap.dto;

import java.math.BigDecimal;
import java.util.List;

public class RiskScoreDtos {

    public record RiskScoreRequest(
            BigDecimal amount,
            long vendorAgeDays,
            long invoicesLast30Days,
            boolean bankAccountChangedRecently,
            boolean duplicateCandidate,
            double amountZScore
    ) {}

    public record RiskScoreResponse(
            double riskScore,
            boolean anomaly,
            List<String> topFactors
    ) {}
}
