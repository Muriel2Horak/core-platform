package cz.muriel.core.config;

import cz.muriel.core.auth.TenantLoggingInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 🌐 Web Configuration Registers interceptors for tenant-aware logging and
 * other web concerns
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Autowired
  private TenantLoggingInterceptor tenantLoggingInterceptor;

  @Override
  public void addInterceptors(@NonNull InterceptorRegistry registry) {
    // Register tenant logging interceptor for all requests
    registry.addInterceptor(tenantLoggingInterceptor).addPathPatterns("/**").excludePathPatterns(
        "/actuator/**", // Exclude actuator endpoints
        "/error/**", // Exclude error pages
        "/static/**", // Exclude static resources
        "/favicon.ico" // Exclude favicon
    );
  }
}
