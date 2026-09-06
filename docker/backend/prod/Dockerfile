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
    --mount=type=cache,target=/app/.gradle \
    ./gradlew bootJar --no-daemon -Pproduction

RUN java -Djarmode=tools -jar build/libs/*.jar extract --layers --destination extracted

FROM amazoncorretto:24-headless
WORKDIR /app

ENV USER_ID=1000
ENV GROUP_ID=1000

RUN dnf install -y shadow-utils && \
    groupadd -g ${GROUP_ID} spring && \
    useradd -r -u ${USER_ID} -g spring -m -s /bin/bash spring && \
    dnf clean all && \
    chown -R spring:spring /app

USER spring:spring

COPY --from=builder /app/extracted/dependencies/ ./
COPY --from=builder /app/extracted/spring-boot-loader/ ./
COPY --from=builder /app/extracted/snapshot-dependencies/ ./
COPY --from=builder /app/extracted/application/ ./

CMD ["java", "-XX:+UseG1GC", "-Xmx512m", "org.springframework.boot.loader.launch.JarLauncher"]
