package com.sentinelap.service;

import com.sentinelap.dto.RiskScoreDtos.RiskScoreRequest;
import com.sentinelap.dto.RiskScoreDtos.RiskScoreResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Talks to the Python ML scoring service (isolation-forest anomaly model).
 * Fails soft: if the ML service is unreachable, callers fall back to the
 * rules-engine-only score rather than blocking invoice processing.
 */
@Service
@Slf4j
public class RiskScoringClient {

    private final RestClient mlServiceRestClient;

    public RiskScoringClient(RestClient mlServiceRestClient) {
        this.mlServiceRestClient = mlServiceRestClient;
    }

    public RiskScoreResponse score(RiskScoreRequest request) {
        try {
            RiskScoreResponse response = mlServiceRestClient.post()
                    .uri("/score")
                    .body(request)
                    .retrieve()
                    .body(RiskScoreResponse.class);
            return response != null ? response : fallback();
        } catch (Exception ex) {
            log.warn("ML scoring service unavailable, falling back to rules-only score: {}", ex.getMessage());
            return fallback();
        }
    }

    private RiskScoreResponse fallback() {
        return new RiskScoreResponse(0.0, false, List.of("ml-service-unavailable"));
    }
}
