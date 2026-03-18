FROM eclipse-temurin:21-jdk-alpine AS build

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

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S app && adduser -S app -G app

WORKDIR /app

COPY --from=build /workspace/api/build/libs/*.jar app.jar

RUN chown -R app:app /app
USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
