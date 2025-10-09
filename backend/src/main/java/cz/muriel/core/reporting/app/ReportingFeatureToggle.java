package cz.muriel.core.reporting.app;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for checking feature toggles in the Reporting module.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportingFeatureToggle {

    private final ReportingProperties properties;

    /**
     * Check if reporting is enabled.
     */
    public boolean isReportingEnabled() {
        return properties.isEnabled();
    }

    /**
     * Check if reporting is enabled and throw exception if not.
     * 
     * @throws IllegalStateException if reporting is disabled
     */
    public void requireReportingEnabled() {
        if (!isReportingEnabled()) {
            throw new IllegalStateException("Reporting module is disabled");
        }
    }

    /**
     * Check if Redis cache is enabled.
     */
    public boolean isRedisCacheEnabled() {
        return "redis".equalsIgnoreCase(properties.getCache().getProvider());
    }

    /**
     * Check if Caffeine cache is enabled.
     */
    public boolean isCaffeineCacheEnabled() {
        return "caffeine".equalsIgnoreCase(properties.getCache().getProvider());
    }
}
