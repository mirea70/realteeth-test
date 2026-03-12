FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

COPY gradle gradle
COPY gradlew build.gradle settings.gradle ./
COPY modules modules/

# Give execution rights on the gradle wrapper
RUN chmod +x ./gradlew

# Build the specified module's bootJar
ARG MODULE_NAME
RUN ./gradlew :${MODULE_NAME}:bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

ARG MODULE_NAME
COPY --from=builder /app/modules/${MODULE_NAME}/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
