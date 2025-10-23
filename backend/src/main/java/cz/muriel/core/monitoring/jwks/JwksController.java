package cz.muriel.core.monitoring.jwks;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * JWKS endpoint for Grafana JWT verification
 * 
 * Exposes BFF's public key so Grafana can verify JWT signatures
 * 
 * Security: - Public endpoint (no auth required) - Read-only (GET only) - No
 * sensitive data (only public key) - Cached by Grafana (TTL: 5 min)
 */
@RestController @RequestMapping("/.well-known") @Slf4j
public class JwksController {

  private final RSAKey publicJwk;

  public JwksController(JwksKeyProvider keyProvider) {
    this.publicJwk = keyProvider.getPublicJwk();
    log.info("✅ JWKS endpoint initialized with kid={}", publicJwk.getKeyID());
  }

  /**
   * Standard JWKS endpoint for JWT verification
   * 
   * Returns public key in JWK format Format:
   * https://datatracker.ietf.org/doc/html/rfc7517
   */
  @GetMapping("/jwks.json")
  public Map<String, Object> getJwks() {
    JWKSet jwkSet = new JWKSet(publicJwk);
    return jwkSet.toJSONObject();
  }
}
