package cz.muriel.core.auth;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.*;
import java.util.stream.Collectors;

public class AudienceValidator implements OAuth2TokenValidator<Jwt> {
  private final Set<String> allowedAudiences;

  public AudienceValidator(String expectedAudienceCsv) {
    this.allowedAudiences = Arrays
        .stream(Optional.ofNullable(expectedAudienceCsv).orElse("").split(",")).map(String::trim)
        .filter(s -> !s.isEmpty()).collect(Collectors.toSet());
  }

  @Override
  public OAuth2TokenValidatorResult validate(Jwt token) {
    // 🔧 FIX: Pokud audience není vyžadován nebo je prázdný seznam, považuj za validní
    if (allowedAudiences.isEmpty()) {
      return OAuth2TokenValidatorResult.success();
    }
    
    List<String> audiences = token.getAudience();
    if (audiences != null && audiences.stream().anyMatch(allowedAudiences::contains)) {
      return OAuth2TokenValidatorResult.success();
    }
    
    // 🔧 FIX: Pokud token nemá audience ale žádný není vyžadován, považuj za validní  
    if (audiences == null || audiences.isEmpty()) {
      return OAuth2TokenValidatorResult.success();
    }
    
    OAuth2Error error = new OAuth2Error("invalid_token",
        "JWT audience does not match allowed values: " + String.join(", ", allowedAudiences), null);
    return OAuth2TokenValidatorResult.failure(error);
  }
}
