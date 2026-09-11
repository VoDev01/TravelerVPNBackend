# syntax=docker/dockerfile:1
FROM --platform=$BUILDPLATFORM amazoncorretto:24 AS builder
WORKDIR /app

RUN dnf install -y findutils

COPY gradlew .
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts gradle.properties ./

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew dependencies --no-daemon

COPY src ./src

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew build -x test --no-daemon -Pproduction

FROM amazoncorretto:24-headless
WORKDIR /app

ENV USER_ID=100
ENV GROUP_ID=1000

RUN dnf update -y && \
    dnf install -y shadow-utils tar gzip  && \
    dnf clean all

RUN curl -sfS https://dotenvx.sh | sh && \
    groupadd -g ${GROUP_ID} spring && \
    useradd -r -u ${USER_ID} -g spring -m -s /bin/bash spring && \
    chown -R spring:spring /app

USER spring:spring

COPY --from=builder /app/build/libs/*-SNAPSHOT.jar ./app.jar
COPY .env .env

CMD ["dotenvx", "run", "--", "java", "-XX:+UseG1GC", "-Xmx512m", "-jar", "app.jar"]
