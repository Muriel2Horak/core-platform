package cz.muriel.core.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * AI Metrics Collector
 * 
 * Exports AI-related metrics to Prometheus.
 * Tracks requests, errors, MCP calls, policy denials.
 * 
 * @since 2025-10-14
 */
@Slf4j
@Component
public class AiMetricsCollector {
  
  private final MeterRegistry meterRegistry;
  
  public AiMetricsCollector(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }
  
  /**
   * Record AI request
   * 
   * @param tenantId Tenant ID
   * @param route Route ID
   * @param mode AI mode (META_ONLY, REDACTED, FULL)
   */
  public void recordAiRequest(String tenantId, String route, String mode) {
    Counter.builder("ai_requests_total")
        .description("Total number of AI context requests")
        .tags(Tags.of(
            "tenant", tenantId != null ? tenantId : "unknown",
            "route", route != null ? route : "unknown",
            "mode", mode != null ? mode : "unknown"
        ))
        .register(meterRegistry)
        .increment();
  }
  
  /**
   * Record AI error
   * 
   * @param errorType Error type (DISABLED, ROUTE_NOT_FOUND, INTERNAL, etc.)
   */
  public void recordAiError(String errorType) {
    Counter.builder("ai_errors_total")
        .description("Total number of AI errors")
        .tags(Tags.of("type", errorType != null ? errorType : "unknown"))
        .register(meterRegistry)
        .increment();
  }
  
  /**
   * Record MCP tool call
   * 
   * @param tool Tool name (ui_context, wf_context, auth, data_context)
   */
  public void recordMcpCall(String tool) {
    Counter.builder("mcp_calls_total")
        .description("Total number of MCP tool calls")
        .tags(Tags.of("tool", tool != null ? tool : "unknown"))
        .register(meterRegistry)
        .increment();
  }
  
  /**
   * Record AI help request
   * 
   * @param route Route ID
   */
  public void recordAiHelpRequest(String route) {
    Counter.builder("ai_help_requests_total")
        .description("Total number of AI help widget requests")
        .tags(Tags.of("route", route != null ? route : "unknown"))
        .register(meterRegistry)
        .increment();
  }
  
  /**
   * Record AI policy denial
   */
  public void recordPolicyDenial() {
    Counter.builder("ai_policy_denied_total")
        .description("Total number of AI policy denials (PII, visibility, etc.)")
        .register(meterRegistry)
        .increment();
  }
  
  /**
   * Record AI tokens (stub for future)
   * 
   * @param type "input" or "output"
   * @param count Token count
   */
  public void recordAiTokens(String type, long count) {
    // Stub - will be implemented when AI model integration is added
    Counter.builder("ai_tokens_total")
        .description("Total number of AI tokens (stub)")
        .tags(Tags.of("type", type != null ? type : "unknown"))
        .register(meterRegistry)
        .increment(count);
  }
}
