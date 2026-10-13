# syntax=docker/dockerfile:1

# 1. Build the web panel
FROM node:22-alpine AS web
WORKDIR /build
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# 2. Build the API, bundling the panel as static resources
FROM eclipse-temurin:25-jdk-alpine AS api
WORKDIR /build
COPY api/ ./
COPY --from=web /build/dist/ src/main/resources/static/
RUN sh mvnw -B -q -DskipTests package && cp target/mvp-*.jar /build/app.jar

# 3. Runtime image
FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S app && adduser -S app -G app && mkdir /data && chown app:app /data
USER app
WORKDIR /app
COPY --from=api /build/app.jar app.jar
ENV SPRING_PROFILES_ACTIVE=prod \
    APP_DATA_DIR=/data \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
VOLUME /data
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health >/dev/null || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
