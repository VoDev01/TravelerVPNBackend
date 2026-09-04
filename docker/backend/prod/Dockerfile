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

RUN dnf install -y shadow-utils

RUN groupadd -r vault && useradd -r spring -g vault && \
chown -R spring:vault .

USER spring:vault

COPY --from=builder /app/build/libs/*-plain.jar app.jar

CMD ["java", "-XX:+UseG1GC", "-Xmx512m", "-jar", "app.jar"]
