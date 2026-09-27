package com.sentinelap.integration.quickbooks;

import com.fasterxml.jackson.databind.JsonNode;
import com.sentinelap.config.QuickBooksProperties;
import com.sentinelap.domain.QuickBooksConnection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;

/**
 * Thin client for Intuit's QuickBooks Online OAuth2 + REST API.
 *
 * This talks to the REAL Intuit endpoints (sandbox by default). To actually use it you need
 * your own free Intuit developer app (https://developer.intuit.com) and a sandbox company,
 * with SENTINELAP_QBO_* environment variables set - see README "QuickBooks integration" section.
 * With sentinelap.quickbooks.enabled=false (the default), none of this code path is exercised
 * and the app runs entirely on synthetic data.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class QuickBooksClient {

    private static final String AUTHORIZE_URL = "https://appcenter.intuit.com/connect/oauth2";
    private static final String TOKEN_URL = "https://oauth.platform.intuit.com/oauth2/v1/tokens/bearer";
    private static final String SANDBOX_API_BASE = "https://sandbox-quickbooks.api.intuit.com";
    private static final String PRODUCTION_API_BASE = "https://quickbooks.api.intuit.com";

    private final QuickBooksProperties properties;
    private final RestClient quickBooksRestClient;

    public String buildAuthorizationUrl(String state) {
        return UriComponentsBuilder.fromHttpUrl(AUTHORIZE_URL)
                .queryParam("client_id", properties.getClientId())
                .queryParam("response_type", "code")
                .queryParam("scope", "com.intuit.quickbooks.accounting")
                .queryParam("redirect_uri", properties.getRedirectUri())
                .queryParam("state", state)
                .build()
                .toUriString();
    }

    public QuickBooksConnection exchangeCodeForTokens(String authCode, String realmId) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", authCode);
        form.add("redirect_uri", properties.getRedirectUri());

        JsonNode response = quickBooksRestClient.post()
                .uri(TOKEN_URL)
                .header(HttpHeaders.AUTHORIZATION, basicAuthHeader())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .body(form)
                .retrieve()
                .body(JsonNode.class);

        OffsetDateTime now = OffsetDateTime.now();
        return QuickBooksConnection.builder()
                .realmId(realmId)
                .accessToken(response.get("access_token").asText())
                .refreshToken(response.get("refresh_token").asText())
                .accessTokenExpiresAt(now.plusSeconds(response.get("expires_in").asLong()))
                .refreshTokenExpiresAt(now.plusSeconds(response.get("x_refresh_token_expires_in").asLong()))
                .build();
    }

    public QuickBooksConnection refreshTokens(QuickBooksConnection connection) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", connection.getRefreshToken());

        JsonNode response = quickBooksRestClient.post()
                .uri(TOKEN_URL)
                .header(HttpHeaders.AUTHORIZATION, basicAuthHeader())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .body(form)
                .retrieve()
                .body(JsonNode.class);

        OffsetDateTime now = OffsetDateTime.now();
        connection.setAccessToken(response.get("access_token").asText());
        connection.setRefreshToken(response.get("refresh_token").asText());
        connection.setAccessTokenExpiresAt(now.plusSeconds(response.get("expires_in").asLong()));
        connection.setRefreshTokenExpiresAt(now.plusSeconds(response.get("x_refresh_token_expires_in").asLong()));
        return connection;
    }

    public JsonNode query(QuickBooksConnection connection, String sqlLikeQuery) {
        String base = "sandbox".equalsIgnoreCase(properties.getEnvironment()) ? SANDBOX_API_BASE : PRODUCTION_API_BASE;
        String url = UriComponentsBuilder.fromHttpUrl(base + "/v3/company/{realmId}/query")
                .queryParam("query", sqlLikeQuery)
                .buildAndExpand(connection.getRealmId())
                .toUriString();

        return quickBooksRestClient.get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + connection.getAccessToken())
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .body(JsonNode.class);
    }

    public static String newState() {
        return UUID.randomUUID().toString();
    }

    private String basicAuthHeader() {
        String creds = properties.getClientId() + ":" + properties.getClientSecret();
        return "Basic " + Base64.getEncoder().encodeToString(creds.getBytes(StandardCharsets.UTF_8));
    }
}
