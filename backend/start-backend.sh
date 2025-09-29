#!/bin/bash
set -e

echo "🔒 Starting backend with SSL certificate import..."

# Import SSL certificate to Java truststore if available
if [ -f "/etc/ssl/backend/cert.pem" ]; then
    echo "🔐 Creating custom truststore with SSL certificate..."
    
    # Create custom truststore in /tmp (writable)
    CUSTOM_TRUSTSTORE="/tmp/custom-truststore.jks"
    
    # Copy default truststore to writable location
    cp $JAVA_HOME/lib/security/cacerts $CUSTOM_TRUSTSTORE
    
    # Import the certificate into custom truststore
    keytool -import -trustcacerts -keystore $CUSTOM_TRUSTSTORE \
        -storepass changeit -noprompt -alias keycloak-ssl \
        -file /etc/ssl/backend/cert.pem || echo "⚠️ Certificate import failed or already exists"
    
    # Set Java system properties to use custom truststore
    export JAVA_OPTS="$JAVA_OPTS -Djavax.net.ssl.trustStore=$CUSTOM_TRUSTSTORE -Djavax.net.ssl.trustStorePassword=changeit"
    
    echo "✅ SSL certificate imported to custom truststore successfully"
else
    echo "⚠️ SSL certificate not found at /etc/ssl/backend/cert.pem"
fi

# Start the Java application
echo "🚀 Starting Spring Boot application..."
exec java $JAVA_OPTS -jar /app/app.jar