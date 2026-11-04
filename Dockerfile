# syntax=docker/dockerfile:1.7
# ONE Dockerfile for every Java service:
#   docker build --build-arg SERVICE=booking-service -t grand-hotel/booking-service .
ARG SERVICE

FROM maven:3.9-eclipse-temurin-21 AS build
ARG SERVICE
WORKDIR /src
# 1) POMs first -> the dependency layer stays cached until a pom changes
COPY pom.xml .
COPY libs/common-lib/pom.xml libs/common-lib/
COPY services/service-parent/pom.xml services/service-parent/
COPY services/auth-service/pom.xml services/auth-service/
COPY services/fx-service/pom.xml services/fx-service/
COPY services/pricing-service/pom.xml services/pricing-service/
COPY services/booking-service/pom.xml services/booking-service/
COPY services/gateway-service/pom.xml services/gateway-service/
COPY coverage-report/pom.xml coverage-report/
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -pl services/${SERVICE} -am dependency:go-offline -DskipTests || true
# 2) Sources
COPY libs libs
COPY services services
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -pl services/${SERVICE} -am package -DskipTests \
 && cp services/${SERVICE}/target/${SERVICE}-*.jar /app.jar \
 && java -Djarmode=tools -jar /app.jar extract --layers --launcher --destination /layers

FROM eclipse-temurin:21-jre-alpine
RUN apk upgrade --no-cache && addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build --chown=app:app /layers/dependencies/ ./
COPY --from=build --chown=app:app /layers/spring-boot-loader/ ./
COPY --from=build --chown=app:app /layers/snapshot-dependencies/ ./
COPY --from=build --chown=app:app /layers/application/ ./
USER app
ENV SERVER_PORT=8080 \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=10 \
  CMD wget -qO- "http://127.0.0.1:${SERVER_PORT}/actuator/health/readiness" >/dev/null || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
