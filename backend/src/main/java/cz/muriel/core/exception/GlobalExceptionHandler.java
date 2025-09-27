package cz.muriel.core.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j @RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
    log.warn("Validation error: {}", ex.getMessage());

    ErrorResponse error = ErrorResponse.builder().timestamp(Instant.now())
        .status(HttpStatus.BAD_REQUEST.value()).error("Bad Request").message(ex.getMessage())
        .build();

    return ResponseEntity.badRequest().body(error);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
      HttpMessageNotReadableException ex) {
    log.warn("Request body not readable: {}", ex.getMessage());

    ErrorResponse error = ErrorResponse.builder().timestamp(Instant.now())
        .status(HttpStatus.BAD_REQUEST.value()).error("Bad Request")
        .message("Neplatný formát požadavku nebo chybějící tělo zprávy").build();

    return ResponseEntity.badRequest().body(error);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationExceptions(
      MethodArgumentNotValidException ex) {
    log.warn("Validation failed: {}", ex.getMessage());

    Map<String, String> fieldErrors = new HashMap<>();
    ex.getBindingResult().getAllErrors().forEach(error -> {
      String fieldName = ((FieldError) error).getField();
      String errorMessage = error.getDefaultMessage();
      fieldErrors.put(fieldName, errorMessage);
    });

    ErrorResponse error = ErrorResponse.builder().timestamp(Instant.now())
        .status(HttpStatus.BAD_REQUEST.value()).error("Validation Failed")
        .message("Validace vstupních dat selhala").fieldErrors(fieldErrors).build();

    return ResponseEntity.badRequest().body(error);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
    log.warn("Access denied: {}", ex.getMessage());

    ErrorResponse error = ErrorResponse.builder().timestamp(Instant.now())
        .status(HttpStatus.FORBIDDEN.value()).error("Access Denied")
        .message("Nemáte oprávnění k této operaci").build();

    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
    log.error("Runtime error: {}", ex.getMessage(), ex);

    // Keycloak specific error handling
    if (ex.getMessage() != null) {
      if (ex.getMessage().contains("User not found")) {
        ErrorResponse error = ErrorResponse.builder().timestamp(Instant.now())
            .status(HttpStatus.NOT_FOUND.value()).error("Not Found")
            .message("Uživatel nebyl nalezen").build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
      }

      if (ex.getMessage().contains("Role not found")) {
        ErrorResponse error = ErrorResponse.builder().timestamp(Instant.now())
            .status(HttpStatus.NOT_FOUND.value()).error("Not Found").message("Role nebyla nalezena")
            .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
      }

      if (ex.getMessage().contains("Failed to authenticate")) {
        ErrorResponse error = ErrorResponse.builder().timestamp(Instant.now())
            .status(HttpStatus.UNAUTHORIZED.value()).error("Authentication Failed")
            .message("Chyba autentifikace s Keycloak").build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
      }
    }

    ErrorResponse error = ErrorResponse.builder().timestamp(Instant.now())
        .status(HttpStatus.INTERNAL_SERVER_ERROR.value()).error("Internal Server Error")
        .message("Došlo k neočekávané chybě").build();

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
    log.error("Unexpected error: {}", ex.getMessage(), ex);

    ErrorResponse error = ErrorResponse.builder().timestamp(Instant.now())
        .status(HttpStatus.INTERNAL_SERVER_ERROR.value()).error("Internal Server Error")
        .message("Došlo k neočekávané chybě").build();

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }
}
