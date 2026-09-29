# syntax=docker/dockerfile:1.7

# ==============================================================================
# assignment-service - Spring Boot 4.1 / Java 25 / Maven (single module)
#
# Build context expected from the repository root:
#   docker build -f deploy/Dockerfile -t assignment-service .
#
# To run tests as part of the image build:
#   docker build --build-arg SKIP_TESTS=false -f deploy/Dockerfile .
# ==============================================================================

ARG JAVA_VERSION=25

# ------------------------------------------------------------------------------
# Stage 1: Resolve dependencies, compile, package and extract Spring Boot layers
# ------------------------------------------------------------------------------
FROM eclipse-temurin:${JAVA_VERSION}-jdk-jammy AS builder

WORKDIR /workspace

# Copy only Maven metadata first. Changes in source code will not invalidate the
# dependency layer. The BuildKit cache avoids downloading dependencies again.
COPY --chmod=0755 mvnw ./mvnw
COPY .mvn ./.mvn
COPY pom.xml ./pom.xml

RUN --mount=type=cache,id=maintenance-service-maven,target=/root/.m2,sharing=locked \
    ./mvnw --batch-mode --no-transfer-progress -DskipTests dependency:go-offline

# This repository is a single Maven module, so only its source tree is required.
COPY src ./src

# Tests normally run in the CI quality stage. Set SKIP_TESTS=false when the
# Docker build itself must execute them.
ARG SKIP_TESTS=true
RUN --mount=type=cache,id=maintenance-service-maven,target=/root/.m2,sharing=locked \
    if [ "${SKIP_TESTS}" = "true" ]; then \
        ./mvnw --batch-mode --no-transfer-progress -Dmaven.test.skip=true package; \
    else \
        ./mvnw --batch-mode --no-transfer-progress verify; \
    fi \
    && cp target/maintenance-service-*.jar application.jar \
    && java -Djarmode=tools -jar application.jar extract \
        --layers \
        --launcher \
        --destination /workspace/extracted \
        --force

# ------------------------------------------------------------------------------
# Stage 2: Minimal runtime image
# ------------------------------------------------------------------------------
FROM eclipse-temurin:${JAVA_VERSION}-jre-jammy AS runtime

LABEL org.opencontainers.image.title="maintenance-service" \
      org.opencontainers.image.description="Maintenance Service - Spring Boot" \
      org.opencontainers.image.vendor="Cibertec Grupo 10"

# Fixed UID/GID works well with Docker, Kubernetes and mounted volumes. No extra
# OS packages are installed, keeping the runtime image smaller and reducing its
# attack surface.
RUN groupadd --gid 10001 spring \
    && useradd --uid 10001 --gid spring --home-dir /app \
        --shell /usr/sbin/nologin --no-create-home spring

WORKDIR /app

# Copy in order of expected change frequency to maximize registry layer reuse.
COPY --from=builder --chown=10001:10001 /workspace/extracted/dependencies/ ./
COPY --from=builder --chown=10001:10001 /workspace/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=10001:10001 /workspace/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=10001:10001 /workspace/extracted/application/ ./

USER 10001:10001

# application.properties does not override server.port, so Spring Boot uses 8080.
EXPOSE 8080

# Java 25 is container-aware. Percentages adapt the heap to the memory limit
# assigned by Docker/Kubernetes without hardcoding -Xms or -Xmx.
ENV JAVA_TOOL_OPTIONS="-XX:InitialRAMPercentage=25.0 -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

# No Docker HEALTHCHECK is declared because the application currently has no
# Actuator/readiness endpoint. Configure the orchestrator probe when such an
# endpoint is added instead of treating a generic 404 as application health.
STOPSIGNAL SIGTERM

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
