# syntax=docker/dockerfile:1
FROM --platform=$BUILDPLATFORM amazoncorretto:24
WORKDIR /app
RUN dnf install -y findutils

COPY build.gradle.kts settings.gradle.kts ./

RUN gradle testClasses --no-daemon || true
