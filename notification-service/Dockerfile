# Etapa 1: Build
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

COPY pom.xml /app/pom.xml
COPY notification-service/pom.xml /app/notification-service/pom.xml
COPY notification-service/src /app/notification-service/src

RUN mvn -f /app/pom.xml clean package -pl notification-service -am -DskipTests

# Etapa 2: Runtime
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd -r appgroup && useradd -r -g appgroup appuser

COPY --from=builder /app/notification-service/target/*.jar app.jar

RUN chown -R appuser:appgroup /app
USER appuser

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]
