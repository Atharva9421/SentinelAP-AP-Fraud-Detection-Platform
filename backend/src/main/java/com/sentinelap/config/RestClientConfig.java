package com.sentinelap.config;

import java.net.http.HttpClient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient mlServiceRestClient(@Value("${sentinelap.ml-service.base-url}") String baseUrl) {
        String normalized = baseUrl.startsWith("http://") || baseUrl.startsWith("https://")
                ? baseUrl
                : "http://" + baseUrl;

        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        return RestClient.builder()
                .baseUrl(normalized)
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .build();
    }

    @Bean
    public RestClient quickBooksRestClient() {
        return RestClient.builder().build();
    }
}