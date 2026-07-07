# Etapa 1: Build
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

COPY pom.xml /app/pom.xml
COPY inventory-service/pom.xml /app/inventory-service/pom.xml
COPY inventory-service/src /app/inventory-service/src

RUN mvn -f /app/pom.xml clean package -pl inventory-service -am -DskipTests

# Etapa 2: Runtime
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd -r appgroup && useradd -r -g appgroup appuser

COPY --from=builder /app/inventory-service/target/*.jar app.jar

RUN chown -R appuser:appgroup /app
USER appuser

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
