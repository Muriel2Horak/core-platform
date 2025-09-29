package cz.muriel.keycloak.webhook;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class EventWebhookProviderTest {

  @Test
  void shouldComputeHmacSha256Correctly() {
    EventWebhookProvider provider = createTestProvider();

    // Test with known input/output
    String testData = "test message";
    String testSecret = "secret-key";
    byte[] data = testData.getBytes(StandardCharsets.UTF_8);

    String result = provider.computeHmacSha256(data, testSecret);

    // Verify basic properties of HMAC result
    assertNotNull(result);
    assertEquals(64, result.length()); // SHA-256 hex should be 64 chars
    assertTrue(result.matches("[0-9a-f]+"), "Result should be lowercase hex");

    // Test consistency - same input should produce same output
    String result2 = provider.computeHmacSha256(data, testSecret);
    assertEquals(result, result2);
  }

  @Test
  void shouldProduceDifferentHmacForDifferentData() {
    EventWebhookProvider provider = createTestProvider();
    String secret = "same-secret";

    String hmac1 = provider.computeHmacSha256("data1".getBytes(), secret);
    String hmac2 = provider.computeHmacSha256("data2".getBytes(), secret);

    assertNotEquals(hmac1, hmac2);
  }

  @Test
  void shouldProduceDifferentHmacForDifferentSecret() {
    EventWebhookProvider provider = createTestProvider();
    byte[] data = "same data".getBytes();

    String hmac1 = provider.computeHmacSha256(data, "secret1");
    String hmac2 = provider.computeHmacSha256(data, "secret2");

    assertNotEquals(hmac1, hmac2);
  }

  @Test
  void shouldHandleEmptyData() {
    EventWebhookProvider provider = createTestProvider();

    String result = provider.computeHmacSha256(new byte[0], "secret");

    assertNotNull(result);
    assertEquals(64, result.length());
    assertTrue(result.matches("[0-9a-f]+"));
  }

  @Test
  void shouldExtractUserIdFromPath() {
    EventWebhookProvider provider = createTestProvider();

    // Test valid paths
    assertEquals("user-123", provider.extractUserIdFromPath("users/user-123"));
    assertEquals("user-456", provider.extractUserIdFromPath("users/user-456/roles"));
    assertEquals("uuid-789", provider.extractUserIdFromPath("users/uuid-789/groups/add"));

    // Test invalid paths
    assertNull(provider.extractUserIdFromPath(null));
    assertNull(provider.extractUserIdFromPath(""));
    assertNull(provider.extractUserIdFromPath("roles/some-role"));
    assertNull(provider.extractUserIdFromPath("users/"));
    assertNull(provider.extractUserIdFromPath("users"));
  }

  @Test
  void shouldConvertBytesToHex() {
    EventWebhookProvider provider = createTestProvider();

    // Test known byte values
    byte[] bytes = { 0x00, 0x0F, (byte) 0xFF, 0x42 };
    String result = provider.bytesToHex(bytes);

    assertEquals("000fff42", result);
    assertEquals(8, result.length()); // 4 bytes = 8 hex chars
    assertTrue(result.matches("[0-9a-f]+"));
  }

  @Test
  void shouldHandleEmptyByteArray() {
    EventWebhookProvider provider = createTestProvider();

    String result = provider.bytesToHex(new byte[0]);

    assertEquals("", result);
  }

  private EventWebhookProvider createTestProvider() {
    // Create a test provider with minimal configuration
    return new EventWebhookProvider(null, // session - not needed for these tests
        "http://test.com/webhook", "test-secret",
        java.util.Set.of("USER_CREATED", "USER_UPDATED", "USER_DELETED"));
  }
}
