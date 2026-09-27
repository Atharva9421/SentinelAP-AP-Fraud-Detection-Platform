# --- Build stage ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache dependencies separately from source so a source-only change doesn't
# re-download the whole dependency tree on every rebuild.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B clean package -DskipTests

# --- Runtime stage ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S sentinelap && adduser -S sentinelap -G sentinelap
COPY --from=build /app/target/sentinelap-backend.jar app.jar
USER sentinelap

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
