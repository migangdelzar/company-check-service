package com.incode.verification.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;

class ObservabilityConfigurationTest {
  @Test
  void exposesOnlySafeReadOnlyActuatorMetricsEndpoints() throws IOException {
    var propertySources =
        new YamlPropertySourceLoader()
            .load("application", new ClassPathResource("application.yml"));
    var propertySource = propertySources.stream().findFirst().orElseThrow();

    assertEquals(
        "health,info,metrics,prometheus",
        propertySource.getProperty("management.endpoints.web.exposure.include"));
    assertEquals("never", propertySource.getProperty("management.endpoint.health.show-details"));
    assertEquals(
        "read-only", propertySource.getProperty("management.endpoints.access.max-permitted"));
    assertEquals(
        "${MANAGEMENT_OTLP_METRICS_EXPORT_ENABLED:false}",
        propertySource.getProperty("management.otlp.metrics.export.enabled"));
    assertEquals("logstash", propertySource.getProperty("logging.structured.format.console"));
    assertNull(propertySource.getProperty("management.endpoint.env.access"));
  }
}
