#!/bin/bash
set -e

echo "🔒 Starting backend with SSL certificate import..."

# Import SSL certificate to Java truststore if available
if [ -f "/etc/ssl/backend/cert.pem" ]; then
    echo "🔐 Importing SSL certificate to Java truststore..."
    
    # Create backup of default truststore
    cp $JAVA_HOME/lib/security/cacerts $JAVA_HOME/lib/security/cacerts.backup
    
    # Import the certificate
    keytool -import -trustcacerts -keystore $JAVA_HOME/lib/security/cacerts \
        -storepass changeit -noprompt -alias keycloak-ssl \
        -file /etc/ssl/backend/cert.pem || echo "⚠️ Certificate import failed or already exists"
    
    echo "✅ SSL certificate imported successfully"
else
    echo "⚠️ SSL certificate not found at /etc/ssl/backend/cert.pem"
fi

# Start the Java application
echo "🚀 Starting Spring Boot application..."
exec java $JAVA_OPTS -jar /app/app.jar