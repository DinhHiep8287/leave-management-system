# ===== Build stage =====
FROM gradle:8.10-jdk21-alpine AS builder
WORKDIR /app

COPY --chown=gradle:gradle build.gradle.kts settings.gradle.kts gradle.properties ./
RUN gradle --no-daemon dependencies --refresh-dependencies || true

COPY --chown=gradle:gradle src src
RUN gradle --no-daemon bootJar -x test

# ===== Runtime stage =====
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S app && adduser -S -G app app
USER app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
