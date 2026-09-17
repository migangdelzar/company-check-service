package com.incode.verification.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class InboundRateLimitPropertiesTest {
  @Test
  void exposesTheBackendServiceAdmissionSettings() {
    var properties = new InboundRateLimitProperties(100, Duration.ofSeconds(1), "test:inbound:");

    assertEquals(100, properties.limitForPeriod());
    assertEquals(Duration.ofSeconds(1), properties.refreshPeriod());
    assertEquals("test:inbound:", properties.keyPrefix());
  }
}
