# syntax=docker/dockerfile:1
# check=skip=SecretsUsedInArgOrEnv

FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
COPY src src

# GitHub Packages credentials (for notifications-model) are passed as build args (Render injects env vars this way).
# They're scoped to this build stage only, so they never reach the final runtime image.
ARG BIDDER_USERNAME
ARG BIDDER_GITHUB_TOKEN
RUN --mount=type=cache,target=/root/.gradle \
    test -n "$BIDDER_USERNAME" && test -n "$BIDDER_GITHUB_TOKEN" \
 || { echo "BIDDER_USERNAME and BIDDER_GITHUB_TOKEN build args are required" >&2; exit 1; }; \
    chmod +x gradlew \
 && ./gradlew bootJar --no-daemon -x test \
 && cp "$(ls build/libs/*.jar | grep -v plain)" app.jar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
COPY --from=build --chown=app:app /workspace/app.jar app.jar
USER app

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=90s --retries=3 \
    CMD wget -qO- http://localhost:${PORT:-8080}/api/v1/actuator/health/liveness || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
