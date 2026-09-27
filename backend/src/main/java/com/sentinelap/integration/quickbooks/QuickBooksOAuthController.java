package com.sentinelap.integration.quickbooks;

import com.sentinelap.config.QuickBooksProperties;
import com.sentinelap.domain.QuickBooksConnection;
import com.sentinelap.repo.QuickBooksConnectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.Map;

/**
 * Endpoints that drive the QuickBooks Online OAuth2 "Connect to QuickBooks" flow.
 * All routes are no-ops (403) unless sentinelap.quickbooks.enabled=true and a real
 * Intuit developer app's client id/secret are configured - see README.
 */
@RestController
@RequestMapping("/api/integrations/quickbooks")
@RequiredArgsConstructor
public class QuickBooksOAuthController {

    private final QuickBooksProperties properties;
    private final QuickBooksClient quickBooksClient;
    private final QuickBooksSyncService syncService;
    private final QuickBooksConnectionRepository connectionRepository;

    @GetMapping("/status")
    public Map<String, Object> status() {
        boolean connected = connectionRepository.findFirstByOrderByConnectedAtDesc().isPresent();
        return Map.of(
                "enabled", properties.isEnabled(),
                "connected", connected,
                "environment", properties.getEnvironment()
        );
    }

    @GetMapping("/connect")
    public ResponseEntity<Void> connect() {
        requireEnabled();
        String url = quickBooksClient.buildAuthorizationUrl(QuickBooksClient.newState());
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build();
    }

    @GetMapping("/callback")
    public ResponseEntity<String> callback(@RequestParam("code") String code,
                                            @RequestParam("realmId") String realmId,
                                            @RequestParam(value = "state", required = false) String state) {
        requireEnabled();
        QuickBooksConnection connection = quickBooksClient.exchangeCodeForTokens(code, realmId);
        connectionRepository.save(connection);
        return ResponseEntity.ok("QuickBooks connected successfully for realm " + realmId +
                ". You can close this tab and return to the SentinelAP dashboard.");
    }

    @PostMapping("/sync")
    public QuickBooksSyncService.SyncResult sync() {
        requireEnabled();
        QuickBooksConnection connection = connectionRepository.findFirstByOrderByConnectedAtDesc()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No QuickBooks connection found - connect first"));
        return syncService.syncAll(connection);
    }

    private void requireEnabled() {
        if (!properties.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "QuickBooks integration is disabled. Set SENTINELAP_QBO_ENABLED=true plus client id/secret to enable it.");
        }
    }
}
