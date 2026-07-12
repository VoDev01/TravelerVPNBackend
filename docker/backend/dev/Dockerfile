# syntax=docker/dockerfile:1
FROM amazoncorretto:24
WORKDIR /app
RUN dnf install -y findutils
COPY gradlew .
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts ./
COPY . .
CMD ["./gradlew", "-t", "bootRun"]