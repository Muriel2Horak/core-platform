package cz.muriel.core.test.wiremock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.*;

/**
 * JUnit 5 Extension for WireMock Server
 * 
 * Starts WireMock on random port before tests and stops after. Sets system
 * property WIREMOCK_PORT so Spring can inject it into configuration.
 * 
 * Usage:
 * 
 * <pre>
 * &#64;SpringBootTest(webEnvironment = RANDOM_PORT)
 * &#64;ActiveProfiles("test")
 * &#64;ExtendWith(WireMockExtension.class)
 * class MyIntegrationTest {
 * 
 *   &#64;Autowired
 *   private WireMockServer wireMockServer;
 * 
 *   &#64;Test
 *   void testWithMock() {
 *     wireMockServer.stubFor(get("/api/test").willReturn(ok("response")));
 *     // ... test code
 *   }
 * }
 * </pre>
 */
@Slf4j
public class WireMockExtension implements BeforeAllCallback, AfterAllCallback,
    ParameterResolver {

  private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(WireMockExtension.class);
  private static final String WIREMOCK_SERVER_KEY = "wireMockServer";

  @Override
  public void beforeAll(ExtensionContext context) {
    WireMockServer wireMockServer = new WireMockServer(
        WireMockConfiguration.options()
            .dynamicPort()  // Random port
            .disableRequestJournal()  // Better performance in tests
    );

    wireMockServer.start();
    int port = wireMockServer.port();

    // Set system property so Spring can use it in @Value("${WIREMOCK_PORT}")
    System.setProperty("WIREMOCK_PORT", String.valueOf(port));

    // Store in extension context
    context.getStore(NAMESPACE).put(WIREMOCK_SERVER_KEY, wireMockServer);

    log.info("WireMock server started on port {}", port);
  }

  @Override
  public void afterAll(ExtensionContext context) {
    WireMockServer wireMockServer = context.getStore(NAMESPACE).get(WIREMOCK_SERVER_KEY, WireMockServer.class);
    
    if (wireMockServer != null) {
      wireMockServer.stop();
      System.clearProperty("WIREMOCK_PORT");
      log.info("WireMock server stopped");
    }
  }

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    return parameterContext.getParameter().getType().equals(WireMockServer.class);
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    return extensionContext.getStore(NAMESPACE).get(WIREMOCK_SERVER_KEY, WireMockServer.class);
  }
}
