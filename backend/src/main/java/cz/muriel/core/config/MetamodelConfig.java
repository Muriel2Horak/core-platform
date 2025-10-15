package cz.muriel.core.config;

import cz.muriel.core.metamodel.MetamodelLoader;
import cz.muriel.core.metamodel.schema.GlobalMetamodelConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metamodel Configuration
 * 
 * Provides GlobalMetamodelConfig as a singleton Spring bean. This bean is
 * loaded once at startup and injected into controllers/services.
 */
@Slf4j @Configuration @RequiredArgsConstructor
public class MetamodelConfig {

  private final MetamodelLoader metamodelLoader;

  /**
   * Create GlobalMetamodelConfig as a Spring bean
   * 
   * Loaded from classpath:metamodel/global-config.yaml at startup
   */
  @Bean
  public GlobalMetamodelConfig globalMetamodelConfig() {
    log.info("🔧 Initializing GlobalMetamodelConfig bean");
    GlobalMetamodelConfig config = metamodelLoader.loadGlobalConfig();
    log.info("✅ GlobalMetamodelConfig loaded: streaming.enabled={}, ai.enabled={}",
        config.getStreaming().isEnabled(),
        config.getAi() != null ? config.getAi().getEnabled() : false);
    return config;
  }
}
