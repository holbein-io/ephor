FROM eclipse-temurin:21-jdk-alpine@sha256:bcc7ec7e8fef937ba9f01ee5f810361d722c6b5dbe19ac188ab7b25c1a4dd2c9 AS build

WORKDIR /workspace

COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle build.gradle
COPY gradle.properties gradle.properties
COPY settings.gradle settings.gradle
COPY api/build.gradle api/build.gradle

RUN chmod +x gradlew && ./gradlew :api:dependencies --no-daemon || true

COPY api/src api/src

RUN ./gradlew :api:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine@sha256:ad0cdd9782db550ca7dde6939a16fd850d04e683d37d3cff79d84a5848ba6a5a

RUN addgroup -S app && adduser -S app -G app

WORKDIR /app

COPY --from=build /workspace/api/build/libs/*.jar app.jar

RUN chown -R app:app /app
USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
