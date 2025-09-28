package cz.muriel.keycloak.webhook;

import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * HTTP client for sending webhook requests with retry/backoff logic
 */
public class WebhookClient {

  private static final Logger logger = Logger.getLogger(WebhookClient.class);

  private final HttpClient httpClient;
  private static final int[] RETRY_DELAYS = { 250, 500, 1000 }; // ms

  public WebhookClient() {
    this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(1500)).build();
  }

  /**
   * Send JSON payload with headers and retry logic
   * 
   * @param url target URL
   * @param body JSON body as bytes
   * @param headers HTTP headers
   * @return HTTP response
   * @throws Exception if all retries failed
   */
  public HttpResponse<String> sendJson(String url, byte[] body, Map<String, String> headers)
      throws Exception {
    URI uri = URI.create(url);

    for (int attempt = 0; attempt <= RETRY_DELAYS.length; attempt++) {
      try {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(uri)
            .timeout(Duration.ofMillis(2000)).POST(HttpRequest.BodyPublishers.ofByteArray(body));

        // Add headers
        for (Map.Entry<String, String> header : headers.entrySet()) {
          requestBuilder.header(header.getKey(), header.getValue());
        }

        HttpRequest request = requestBuilder.build();
        HttpResponse<String> response = httpClient.send(request,
            HttpResponse.BodyHandlers.ofString());

        int statusCode = response.statusCode();

        // Success (2xx) or client error (4xx) - don't retry
        if (statusCode < 500) {
          logger.debugf("Webhook request completed: status=%d, attempt=%d", statusCode,
              attempt + 1);
          return response;
        }

        // Server error (5xx) - retry if attempts left
        if (attempt < RETRY_DELAYS.length) {
          int delay = RETRY_DELAYS[attempt];
          logger.warnf("Webhook request failed with status %d, retrying in %dms (attempt %d/%d)",
              statusCode, delay, attempt + 1, RETRY_DELAYS.length + 1);
          Thread.sleep(delay);
        } else {
          logger.errorf("Webhook request failed after %d attempts, final status: %d",
              RETRY_DELAYS.length + 1, statusCode);
          return response;
        }

      } catch (java.net.http.HttpTimeoutException e) {
        // Timeout - retry if attempts left
        if (attempt < RETRY_DELAYS.length) {
          int delay = RETRY_DELAYS[attempt];
          logger.warnf("Webhook request timeout, retrying in %dms (attempt %d/%d): %s", delay,
              attempt + 1, RETRY_DELAYS.length + 1, e.getMessage());
          Thread.sleep(delay);
        } else {
          logger.errorf("Webhook request failed after %d attempts due to timeout",
              RETRY_DELAYS.length + 1);
          throw e;
        }
      } catch (Exception e) {
        // Other exceptions - don't retry
        logger.errorf(e, "Webhook request failed with non-retryable error");
        throw e;
      }
    }

    throw new RuntimeException("Unexpected state - should not reach here");
  }

  public void close() {
    // HttpClient doesn't need explicit closing in Java 11+
  }
}
