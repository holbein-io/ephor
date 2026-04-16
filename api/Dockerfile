FROM eclipse-temurin:21-jdk-alpine@sha256:d702e2d534819da0573af42ee880bfddf6f3497b9e7a3a9f5288a127b231b619 AS build

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

FROM eclipse-temurin:21-jre-alpine@sha256:96c9c9f4da36495d995e4fd849491f9ac53649278bd120a32c3c8860cd5c7096

RUN addgroup -S app && adduser -S app -G app

WORKDIR /app

COPY --from=build /workspace/api/build/libs/*.jar app.jar

RUN chown -R app:app /app
USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
