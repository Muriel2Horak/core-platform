package cz.muriel.core.tenant;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class TenantNotFoundException extends RuntimeException {

  public TenantNotFoundException(String message) {
    super(message);
  }

  public TenantNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}
