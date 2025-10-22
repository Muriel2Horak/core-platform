package cz.muriel.core.monitoring.jwks;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

/**
 * Provides RS256 keypair for Grafana JWT signing
 * 
 * Key rotation:
 * - In-memory keys (generated on startup)
 * - TODO: Load from K8s secret in production
 * - TODO: Support multiple kids for zero-downtime rotation
 * 
 * Security:
 * - Private key never leaves BFF
 * - Public key exposed via JWKS endpoint
 * - 2048-bit RSA (industry standard)
 */
@Component
@Slf4j
public class JwksKeyProvider {

  @Value("${grafana.jwt.key-id:grafana-bff-key-1}")
  private String keyId;

  private RSAKey rsaKey;

  @PostConstruct
  public void init() {
    try {
      // Generate RSA-2048 keypair
      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(2048);
      KeyPair keyPair = generator.generateKeyPair();

      RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
      RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

      // Build JWK with kid
      this.rsaKey = new RSAKey.Builder(publicKey)
          .privateKey(privateKey)
          .keyID(keyId)
          .build();

      log.info("✅ Generated RSA-2048 keypair for Grafana JWT signing (kid={})", keyId);

    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Failed to generate RSA keypair", e);
    }
  }

  /**
   * Get full RSA key (private + public) for signing
   */
  public RSAKey getRsaKey() {
    return rsaKey;
  }

  /**
   * Get public-only JWK for JWKS endpoint
   */
  public RSAKey getPublicJwk() {
    return rsaKey.toPublicJWK();
  }

  /**
   * Get key ID for JWT header
   */
  public String getKeyId() {
    return keyId;
  }
}
