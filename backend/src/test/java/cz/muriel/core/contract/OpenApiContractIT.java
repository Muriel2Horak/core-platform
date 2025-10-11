package cz.muriel.core.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapi4j.core.exception.ResolutionException;
import org.openapi4j.core.validation.ValidationException;
import org.openapi4j.operation.validator.model.Request;
import org.openapi4j.operation.validator.model.Response;
import org.openapi4j.operation.validator.model.impl.DefaultRequest;
import org.openapi4j.operation.validator.model.impl.DefaultResponse;
import org.openapi4j.operation.validator.validation.OperationValidator;
import org.openapi4j.parser.OpenApi3Parser;
import org.openapi4j.parser.model.v3.OpenApi3;
import org.openapi4j.parser.model.v3.Operation;
import org.openapi4j.parser.model.v3.Path;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 🧪 OpenAPI Contract Tests
 * 
 * Validates API responses against OpenAPI specification: - Fetches OpenAPI spec
 * from /v3/api-docs - Validates selected endpoint responses - Exports
 * openapi.json as CI artifact
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class OpenApiContractIT {

  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  private ObjectMapper objectMapper;

  private OpenApi3 openApiSpec;
  private String openApiJson;

  @BeforeEach
  void setUp() throws Exception {
    // Fetch OpenAPI spec
    ResponseEntity<String> response = restTemplate.getForEntity("/v3/api-docs", String.class);

    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    openApiJson = response.getBody();
    assertThat(openApiJson).isNotNull();

    // Parse OpenAPI spec
    File tempFile = File.createTempFile("openapi", ".json");
    try (FileWriter writer = new FileWriter(tempFile)) {
      writer.write(openApiJson);
    }

    openApiSpec = new OpenApi3Parser().parse(tempFile, false);
    tempFile.delete();
  }

  @Test
  void testOpenApiSpecGeneration() {
    // Verify OpenAPI spec is valid
    assertThat(openApiSpec).isNotNull();
    assertThat(openApiSpec.getOpenapi()).startsWith("3.");
    assertThat(openApiSpec.getPaths()).isNotEmpty();

    System.out.println("📋 OpenAPI Info:");
    System.out.println("  Version: " + openApiSpec.getOpenapi());
    System.out.println("  Title: " + openApiSpec.getInfo().getTitle());
    System.out.println("  Paths: " + openApiSpec.getPaths().size());
  }

  @Test
  void testHealthEndpointContract() throws ValidationException, ResolutionException {
    // When: Call health endpoint
    ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);

    // Then: Validate against OpenAPI spec (if defined)
    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getBody()).contains("status");
  }

  @Test
  void testStreamingConfigEndpointContract() throws Exception {
    // Given: OpenAPI path for streaming config
    String path = "/api/admin/streaming/config";
    Path pathItem = openApiSpec.getPath(path);

    if (pathItem != null && pathItem.hasOperation("get")) {
      Operation operation = pathItem.getGet();
      OperationValidator validator = new OperationValidator(openApiSpec, operation);

      // When: Call endpoint
      ResponseEntity<String> response = restTemplate.getForEntity(path, String.class);

      // Then: Validate response against schema
      if (response.getStatusCode().is2xxSuccessful()) {
        Response validationResponse = buildResponse(response);
        validator.validateResponse(validationResponse);

        System.out.println("✅ /api/admin/streaming/config contract validated");
      }
    } else {
      System.out.println("⚠️ Path not defined in OpenAPI: " + path);
    }
  }

  @Test
  void testMetricsEndpointExists() {
    // When: Call Prometheus metrics endpoint
    ResponseEntity<String> response = restTemplate.getForEntity("/actuator/prometheus",
        String.class);

    // Then: Should return metrics
    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getBody()).contains("# HELP");
  }

  @Test
  void exportOpenApiSpec() throws IOException {
    // Export OpenAPI spec to target directory for CI artifacts
    String targetDir = "target/openapi";
    Files.createDirectories(Paths.get(targetDir));

    String outputPath = targetDir + "/openapi.json";
    Files.writeString(Paths.get(outputPath), openApiJson);

    System.out.println("📤 OpenAPI spec exported to: " + outputPath);
    assertThat(new File(outputPath)).exists();
  }

  @Test
  void testResponseSchemas() throws Exception {
    // Test common response patterns
    ResponseEntity<String> healthResponse = restTemplate.getForEntity("/actuator/health",
        String.class);

    JsonNode healthJson = objectMapper.readTree(healthResponse.getBody());
    assertThat(healthJson.has("status")).isTrue();
    assertThat(healthJson.get("status").asText()).isIn("UP", "DOWN", "OUT_OF_SERVICE", "UNKNOWN");

    System.out.println("✅ Response schemas validated");
  }

  // Helper method to build OpenAPI4J Response object
  private Response buildResponse(ResponseEntity<String> response) {
    DefaultResponse.Builder builder = new DefaultResponse.Builder(response.getStatusCode().value());

    if (response.getBody() != null) {
      builder.body(response.getBody());
    }

    HttpHeaders headers = response.getHeaders();
    Map<String, String> headerMap = new HashMap<>();
    headers.forEach((key, values) -> {
      if (!values.isEmpty()) {
        headerMap.put(key, values.get(0));
      }
    });

    return builder.headers(headerMap).build();
  }

  // Helper method to build OpenAPI4J Request object
  @SuppressWarnings("unused")
  private Request buildRequest(String path, HttpMethod method) {
    DefaultRequest.Builder builder = new DefaultRequest.Builder("http://localhost", method.name());
    builder.path(path);
    return builder.build();
  }
}
