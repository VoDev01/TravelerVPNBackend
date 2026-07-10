
FROM --platform=$BUILDPLATFORM amazoncorretto:24 AS builder
WORKDIR /app
RUN dnf install -y findutils
COPY gradlew .
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts ./
RUN ./gradlew help --no-daemon
COPY . .
RUN ./gradlew bootJar --no-daemon
FROM amazoncorretto:24-headless
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-XX:+UseG1GC", "-Xmx512m", "-jar", "app.jar"]
