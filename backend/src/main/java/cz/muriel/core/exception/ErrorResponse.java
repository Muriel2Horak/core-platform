package cz.muriel.core.exception;

import lombok.Builder;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
  private Instant timestamp;
  private int status;
  private String error;
  private String message;
  private String path;
  private Map<String, String> fieldErrors;
}