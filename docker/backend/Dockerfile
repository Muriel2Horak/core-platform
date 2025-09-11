# syntax=docker/dockerfile:1

FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# Tady se už neodkazuj na ../backend, protože context je backend/
COPY pom.xml .
COPY src ./src

RUN mvn -B dependency:go-offline
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080 5005
ENTRYPOINT ["java", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005", "-jar", "app.jar"]