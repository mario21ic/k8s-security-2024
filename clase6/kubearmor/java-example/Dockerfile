# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk-jammy as builder
WORKDIR /build

COPY ./ ./
RUN ./mvnw clean package

FROM eclipse-temurin:21-jre-jammy AS final
RUN apt update && apt install -y curl procps
ARG UID=10001
RUN adduser \
    --disabled-password \
    --gecos "" \
    --home "/nonexistent" \
    --shell "/sbin/nologin" \
    --no-create-home \
    --uid "${UID}" \
    appuser
USER appuser

COPY --from=builder build/target/demo-0.0.1-SNAPSHOT.jar app.jar
COPY ./vault /vault
EXPOSE 8080

ENTRYPOINT [ "java", "-jar", "app.jar" ]
