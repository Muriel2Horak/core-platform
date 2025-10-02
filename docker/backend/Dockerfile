# syntax=docker/dockerfile:1

FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# 📋 Copy Maven settings for reliability
COPY docker/maven-settings-template.xml /root/.m2/settings.xml

# 🏗️ Layer 1: Copy only pom.xml for dependency caching
# Build context je nyní root (.), takže backend soubory jsou v backend/
COPY backend/pom.xml .

# 🚀 Download dependencies with cache mount - better reliability
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -U dependency:go-offline

# 🏗️ Layer 2: Copy source code (separate layer for better cache)
COPY backend/src ./src

# 🚀 Package with cache mount - reuse dependencies
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -U package -DskipTests

FROM eclipse-temurin:21
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# 🔒 SSL: Copy SSL certificates for HTTPS communication with Keycloak  
# Build context je root (.), takže ssl/ je přímo dostupné
COPY ssl/ /etc/ssl/backend/

# 🔒 SSL: Copy startup script for SSL certificate import
# Build context je root (.), takže backend/start-backend.sh je dostupné
COPY backend/start-backend.sh /usr/local/bin/start-backend.sh
RUN chmod +x /usr/local/bin/start-backend.sh

# Spouštěj jako ne-root uživatel, ale nejdříve umožni přístup k Java cacerts
RUN groupadd -r app && useradd -r -g app -u 10001 appuser && chown -R appuser:app /app

# 🔒 SSL: Umožni appuser upravovat Java truststore pro SSL import
RUN chown root:app $JAVA_HOME/lib/security/cacerts && \
  chmod 664 $JAVA_HOME/lib/security/cacerts

USER appuser:app
EXPOSE 8080
ENTRYPOINT ["/usr/local/bin/start-backend.sh"]