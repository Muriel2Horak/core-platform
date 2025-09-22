# syntax=docker/dockerfile:1

FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# 🔧 FIX: Správné cesty k backend souborům z root kontextu
COPY backend/pom.xml .
COPY backend/src ./src

RUN mvn -B dependency:go-offline
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
# Spouštěj jako ne-root uživatel
RUN groupadd -r app && useradd -r -g app -u 10001 appuser && chown -R appuser:app /app
USER appuser:app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]