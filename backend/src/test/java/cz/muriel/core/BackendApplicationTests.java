package cz.muriel.core;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import cz.muriel.core.test.AbstractIntegrationTest;

@SpringBootTest
class BackendApplicationTests extends AbstractIntegrationTest {

	@Test
	void contextLoads() {
		// Test pouze ověří, že aplikační kontext se správně načte
		// PostgreSQL + Redis kontejnery jsou automaticky spuštěny přes
		// AbstractIntegrationTest
	}
}
