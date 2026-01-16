# syntax=docker/dockerfile:1

FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# 📋 Copy Maven settings for reliability
COPY docker/maven-settings-template.xml /root/.m2/settings.xml

# 🏗️ Layer 1: Copy only pom.xml for dependency caching
# Build context je nyní root (.), takže backend soubory jsou v backend/
COPY backend/pom.xml .

# 🚀 Download dependencies with cache mount - better reliability
# ⚠️ TEMPORARILY DISABLED: dependency:go-offline freezes - dependencies will download during package
# RUN --mount=type=cache,target=/root/.m2 \
#   mvn -B -U dependency:go-offline

# 🏗️ Layer 2: Copy source code (separate layer for better cache)
COPY backend/src ./src

# 🚀 Package with cache mount - reuse dependencies
RUN --mount=type=cache,target=/root/.m2 \
  mvn -B -U package -DskipTests -Denforcer.skip=true

FROM eclipse-temurin:21
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# 🔒 SSL: Copy SSL certificates for HTTPS communication with Keycloak  
# Build context je root (.), takže ssl/ je přímo dostupné
COPY ssl/ /etc/ssl/backend/

# 🔒 SSL: Import SSL certificate do Java truststore BĚHEM BUILDU (jako root)
RUN if [ -f /etc/ssl/backend/cert.pem ]; then \
  echo "🔐 Importing SSL certificate to Java truststore during build..."; \
  keytool -importcert -noprompt \
  -alias core-platform-local \
  -file /etc/ssl/backend/cert.pem \
  -keystore $JAVA_HOME/lib/security/cacerts \
  -storepass changeit \
  && echo "✅ SSL certificate imported successfully" \
  || echo "⚠️ SSL certificate import failed (non-critical)"; \
  else \
  echo "⚠️ No SSL certificate found at /etc/ssl/backend/cert.pem"; \
  fi

# 🔒 SSL: Copy startup script (už nebude dělat SSL import, jen spustí aplikaci)
COPY backend/start-backend.sh /usr/local/bin/start-backend.sh
RUN chmod +x /usr/local/bin/start-backend.sh

# Spouštěj jako ne-root uživatel
RUN groupadd -r app && useradd -r -g app -u 10001 appuser && chown -R appuser:app /app

USER appuser:app
EXPOSE 8080
ENTRYPOINT ["/usr/local/bin/start-backend.sh"]
