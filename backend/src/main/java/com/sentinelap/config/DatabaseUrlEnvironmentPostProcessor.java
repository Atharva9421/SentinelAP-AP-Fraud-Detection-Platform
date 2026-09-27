package com.sentinelap.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Managed Postgres add-ons on platforms like Render/Heroku hand you a single
 * connection-string env var (DATABASE_URL, scheme postgres:// or postgresql://)
 * rather than separate host/port/user/password vars. Spring's PostgreSQL JDBC
 * driver needs a jdbc:postgresql:// URL with the credentials passed separately,
 * so this translates one into the other before the context starts, letting
 * render.yaml wire up a single DATABASE_URL from the database's connectionString
 * instead of assembling a JDBC URL by hand.
 *
 * Purely additive: if DATABASE_URL isn't set (local dev, docker-compose, which
 * already provide DB_URL/DB_USER/DB_PASSWORD directly), this is a no-op.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = environment.getProperty("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank()) {
            return;
        }

        try {
            URI uri = new URI(databaseUrl);
            String userInfo = uri.getUserInfo();
            String username = null;
            String password = null;
            if (userInfo != null && userInfo.contains(":")) {
                String[] parts = userInfo.split(":", 2);
                username = parts[0];
                password = parts[1];
            }

            int port = uri.getPort() > 0 ? uri.getPort() : 5432;
            String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + port + uri.getPath();

            Map<String, Object> derived = new LinkedHashMap<>();
            derived.put("spring.datasource.url", jdbcUrl);
            if (username != null) {
                derived.put("spring.datasource.username", username);
            }
            if (password != null) {
                derived.put("spring.datasource.password", password);
            }

            environment.getPropertySources().addFirst(new MapPropertySource("databaseUrlDerived", derived));
        } catch (Exception ex) {
            // Deliberately non-fatal: fall through to whatever spring.datasource.*
            // is otherwise configured, and let normal datasource startup surface
            // a clear error if the configuration turns out to be genuinely broken.
            System.err.println("Could not parse DATABASE_URL, falling back to explicit datasource config: " + ex.getMessage());
        }
    }
}
