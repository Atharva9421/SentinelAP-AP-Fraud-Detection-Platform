package com.sentinelap.web;

import com.sentinelap.dto.AlertDto;
import com.sentinelap.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    public List<AlertDto> list(@RequestParam(required = false) String status) {
        return alertService.listAlerts(status);
    }

    @PatchMapping("/{id}/status")
    public AlertDto updateStatus(@PathVariable UUID id, @RequestBody Map<String, String> body, Authentication auth) {
        String resolvedBy = auth != null ? auth.getName() : "system";
        return alertService.updateStatus(id, body.get("status"), resolvedBy);
    }
}
