# --- Build stage ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /build

# Cache dependencies separately from source so `docker build` doesn't re-download
# the world on every code change.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -q dependency:go-offline

COPY src src
RUN ./mvnw -q -DskipTests clean package

# --- Runtime stage ---
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# curl explicitly installed for the HEALTHCHECK below — not guaranteed present in the base image.
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

RUN useradd --system --create-home --shell /usr/sbin/nologin appuser
COPY --from=build /build/target/*.jar app.jar
# logback-spring.xml writes to a relative logs/ dir (see that file); it must exist
# and be writable by appuser before the app starts, or logging init itself crashes
# the container on boot.
RUN mkdir -p /app/logs && chown -R appuser:appuser /app
USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/api/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
