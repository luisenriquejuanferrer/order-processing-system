# Etapa 1: Build
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Copiar los POMs de todos los módulos para que Maven resuelva el reactor completo
COPY pom.xml /app/pom.xml
COPY api-gateway/pom.xml /app/api-gateway/pom.xml
COPY order-service/pom.xml /app/order-service/pom.xml
COPY payment-service/pom.xml /app/payment-service/pom.xml
COPY inventory-service/pom.xml /app/inventory-service/pom.xml
COPY notification-service/pom.xml /app/notification-service/pom.xml

# Copiar el código fuente del servicio
COPY notification-service/src /app/notification-service/src

RUN mvn -f /app/pom.xml clean package -pl notification-service -am -DskipTests

# Etapa 2: Runtime
FROM eclipse-temurin:21-jre
WORKDIR /app

# curl ya está incluido en la imagen eclipse-temurin:21-jre

RUN groupadd -r appgroup && useradd -r -g appgroup appuser

COPY --from=builder /app/notification-service/target/*.jar app.jar

RUN chown -R appuser:appgroup /app
USER appuser

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]
