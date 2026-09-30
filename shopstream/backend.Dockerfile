# syntax=docker/dockerfile:1
#
# One Dockerfile for every Spring Boot service. Choose the service with a build arg:
#   docker build -f backend.Dockerfile --build-arg SERVICE=order-service -t shopstream/order-service .
#
# Multi-stage build:
#   stage 1 (build)   has Maven + a full JDK and compiles every module
#   stage 2 (runtime) has only a JRE and one jar, so the final image stays small
#
# Stage 1 does not depend on SERVICE, so Docker builds it once and reuses the
# cached result for all seven services.

FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY shopstream-common shopstream-common
COPY api-gateway api-gateway
COPY user-service user-service
COPY product-service product-service
COPY inventory-service inventory-service
COPY order-service order-service
COPY payment-service payment-service
COPY notification-service notification-service

# The cache mount keeps ~/.m2 between builds, so dependencies are downloaded only once.
# Tests are neither compiled nor run here (maven.test.skip): CI runs them with
# `mvn verify` before any image is built.
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn -B -q -Dmaven.test.skip=true package


FROM eclipse-temurin:25-jre AS runtime

# curl is used by the Docker health checks. Run as a non-root user with a numeric
# id, which Kubernetes needs to verify runAsNonRoot.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --no-create-home app

WORKDIR /app
ARG SERVICE
COPY --from=build /workspace/${SERVICE}/target/${SERVICE}.jar app.jar

USER 10001

# The JVM reads the container's memory limit; the heap may use up to 75% of it.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
