package cz.muriel.core.monitoring.grafana;

/**
 * 🚨 GRAFANA PROVISIONING EXCEPTION
 * 
 * Výjimka pro chyby při provisioningu Grafana resources
 */
public class GrafanaProvisioningException extends RuntimeException {

  public GrafanaProvisioningException(String message) {
    super(message);
  }

  public GrafanaProvisioningException(String message, Throwable cause) {
    super(message, cause);
  }
}
