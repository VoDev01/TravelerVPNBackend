# syntax=docker/dockerfile:1
FROM --platform=$BUILDPLATFORM amazoncorretto:24 AS builder
WORKDIR /app
RUN dnf install -y findutils
COPY gradlew .
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts ./
COPY . .
RUN ./gradlew bootJar --no-daemon -Pproduction

FROM amazoncorretto:24-headless
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
RUN find . -type f -exec chown spring:spring {} + && \
    find . -type d -exec chown spring:spring {} +

COPY --from=builder /app/build/libs/*.jar app.jar

CMD ["java", "-XX:+UseG1GC", "-Xmx512m", "-jar", "app.jar"]
