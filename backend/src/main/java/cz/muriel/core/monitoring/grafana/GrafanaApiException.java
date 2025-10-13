package cz.muriel.core.monitoring.grafana;

/**
 * 🚨 GRAFANA API EXCEPTION
 * 
 * Výjimka pro chyby při komunikaci s Grafana API
 */
public class GrafanaApiException extends RuntimeException {

  public GrafanaApiException(String message) {
    super(message);
  }

  public GrafanaApiException(String message, Throwable cause) {
    super(message, cause);
  }
}
