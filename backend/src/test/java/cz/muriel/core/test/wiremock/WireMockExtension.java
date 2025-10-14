package cz.muriel.core.test.wiremock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.*;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * JUnit 5 Extension for WireMock Server
 * 
 * Starts WireMock on random port before Spring context and stops after tests.
 * Injects WireMock URL into Spring properties for Grafana configuration.
 * 
 * Usage:
 * 
 * <pre>
 * &#64;SpringBootTest(webEnvironment = RANDOM_PORT) &#64;ExtendWith(WireMockExtension.class)
 * class MyIntegrationTest {
 * 
 *   &#64;Test
 *   void testWithMock(WireMockServer wireMock) {
 *     wireMock.stubFor(get("/api/test").willReturn(ok("response")));
 *     // ... test code
 *   }
 * }
 * </pre>
 */
@Slf4j
public class WireMockExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver {

  private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
      .create(WireMockExtension.class);
  private static final String WIREMOCK_SERVER_KEY = "wireMockServer";

  public static class WireMockInitializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
      String wireMockPort = System.getProperty("WIREMOCK_PORT");
      if (wireMockPort != null) {
        String wireMockUrl = "http://localhost:" + wireMockPort;
        log.info("🔧 Configuring Grafana to use WireMock at: {}", wireMockUrl);
        System.setProperty("grafana.admin.base-url", wireMockUrl);
        System.setProperty("grafana.client.base-url", wireMockUrl);
      }
    }
  }

  @Override
  public void beforeAll(ExtensionContext context) {
    // Use fixed port 8089 for predictable configuration
    WireMockServer wireMockServer = new WireMockServer(WireMockConfiguration.options().port(8089));

    wireMockServer.start();
    int port = wireMockServer.port();

    // Set system property BEFORE Spring context loads
    System.setProperty("WIREMOCK_PORT", String.valueOf(port));

    // Also set Grafana URLs directly
    String wireMockUrl = "http://localhost:" + port;
    System.setProperty("grafana.admin.base-url", wireMockUrl);
    System.setProperty("grafana.client.base-url", wireMockUrl);

    // Store in extension context
    context.getStore(NAMESPACE).put(WIREMOCK_SERVER_KEY, wireMockServer);

    log.info("✅ WireMock server started on port {}", port);
    log.info("   Grafana URLs configured: {}", wireMockUrl);
  }

  @Override
  public void afterAll(ExtensionContext context) {
    WireMockServer wireMockServer = context.getStore(NAMESPACE).get(WIREMOCK_SERVER_KEY,
        WireMockServer.class);

    if (wireMockServer != null) {
      wireMockServer.stop();
      System.clearProperty("WIREMOCK_PORT");
      System.clearProperty("grafana.admin.base-url");
      System.clearProperty("grafana.client.base-url");
      log.info("WireMock server stopped");
    }
  }

  @Override
  public boolean supportsParameter(ParameterContext parameterContext,
      ExtensionContext extensionContext) {
    return parameterContext.getParameter().getType().equals(WireMockServer.class);
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext,
      ExtensionContext extensionContext) {
    return extensionContext.getStore(NAMESPACE).get(WIREMOCK_SERVER_KEY, WireMockServer.class);
  }
}
