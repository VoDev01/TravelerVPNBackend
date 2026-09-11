# syntax=docker/dockerfile:1
FROM amazoncorretto:24 AS builder
WORKDIR /app

RUN dnf update -y && \
    dnf install -y findutils tar gzip && \
    dnf clean all

RUN curl -sfS https://dotenvx.sh | sh

COPY gradlew .
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY src ./src
COPY .env .env

CMD ["dotenvx", "run", "--", "./gradlew", "-t", "bootRun"]