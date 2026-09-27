package com.sentinelap.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sentinelap.quickbooks")
@Getter
@Setter
public class QuickBooksProperties {
    private boolean enabled;
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String environment = "sandbox";
}
