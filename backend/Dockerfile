# syntax=docker/dockerfile:1.7
# SRM Credit Engine - backend image. Build from the backend/ directory:
#   docker build -t srm-credit-engine-backend .

# ---- Build stage: Maven wrapper, dependency layer cached apart from the sources ----------------
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

# Wrapper and POM first: this layer (and the downloaded dependencies) is reused until pom.xml changes.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:go-offline

COPY src/ src/
# Tests, formatting and coverage run in CI (./mvnw verify); the image build only packages.
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -q package -Dmaven.test.skip=true -Dspotless.check.skip=true -Djacoco.skip=true \
    && java -Djarmode=tools -jar target/credit-engine-*.jar extract --layers --launcher --destination target/extracted

# ---- Runtime stage: JRE only, non-root, layered application --------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S srm && adduser -S -D -H -u 10001 -G srm srm
WORKDIR /app

# Least to most frequently changing layers, so a code change only ships the "application" layer.
COPY --from=build --chown=srm:srm /workspace/target/extracted/dependencies/ ./
COPY --from=build --chown=srm:srm /workspace/target/extracted/spring-boot-loader/ ./
COPY --from=build --chown=srm:srm /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=srm:srm /workspace/target/extracted/application/ ./

USER srm
EXPOSE 8080

# The JVM sizes its heap from the container memory limit (container support is on by default).
ENV SPRING_PROFILES_ACTIVE=docker \
    JAVA_TOOL_OPTIONS="-XX:InitialRAMPercentage=50.0 -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

HEALTHCHECK --interval=15s --timeout=3s --start-period=60s --retries=3 \
    CMD wget -q -O /dev/null http://localhost:8080/actuator/health/liveness || exit 1

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
