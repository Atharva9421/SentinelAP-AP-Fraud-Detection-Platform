package com.sentinelap.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient mlServiceRestClient(@Value("${sentinelap.ml-service.base-url}") String baseUrl) {
        // Render's fromService "hostport" env var value (used for private
        // service-to-service calls) is a bare host:port with no scheme, unlike
        // the http://localhost:8000 used for local dev - tolerate both.
        String normalized = baseUrl.startsWith("http://") || baseUrl.startsWith("https://")
                ? baseUrl
                : "http://" + baseUrl;
        return RestClient.builder().baseUrl(normalized).build();
    }

    @Bean
    public RestClient quickBooksRestClient() {
        return RestClient.builder().build();
    }
}
