package cz.muriel.core.reporting.app;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Tests for ReportingFeatureToggle.
 */
@ExtendWith(MockitoExtension.class)
class ReportingFeatureToggleTest {

  @Mock
  private ReportingProperties properties;

  @InjectMocks
  private ReportingFeatureToggle featureToggle;

  @Test
  void shouldReturnTrueWhenReportingEnabled() {
    when(properties.isEnabled()).thenReturn(true);
    assertThat(featureToggle.isReportingEnabled()).isTrue();
  }

  @Test
  void shouldReturnFalseWhenReportingDisabled() {
    when(properties.isEnabled()).thenReturn(false);
    assertThat(featureToggle.isReportingEnabled()).isFalse();
  }

  @Test
  void shouldNotThrowWhenReportingEnabled() {
    when(properties.isEnabled()).thenReturn(true);
    featureToggle.requireReportingEnabled(); // should not throw
  }

  @Test
  void shouldThrowWhenReportingDisabled() {
    when(properties.isEnabled()).thenReturn(false);
    assertThatThrownBy(() -> featureToggle.requireReportingEnabled())
        .isInstanceOf(IllegalStateException.class).hasMessage("Reporting module is disabled");
  }

  @Test
  void shouldDetectRedisCacheProvider() {
    ReportingProperties.CacheConfig cacheConfig = new ReportingProperties.CacheConfig();
    cacheConfig.setProvider("redis");
    when(properties.getCache()).thenReturn(cacheConfig);

    assertThat(featureToggle.isRedisCacheEnabled()).isTrue();
    assertThat(featureToggle.isCaffeineCacheEnabled()).isFalse();
  }

  @Test
  void shouldDetectCaffeineCacheProvider() {
    ReportingProperties.CacheConfig cacheConfig = new ReportingProperties.CacheConfig();
    cacheConfig.setProvider("caffeine");
    when(properties.getCache()).thenReturn(cacheConfig);

    assertThat(featureToggle.isRedisCacheEnabled()).isFalse();
    assertThat(featureToggle.isCaffeineCacheEnabled()).isTrue();
  }
}
