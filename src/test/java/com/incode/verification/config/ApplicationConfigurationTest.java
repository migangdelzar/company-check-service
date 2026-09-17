package com.incode.verification.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class ApplicationConfigurationTest {
  @Test
  void exposesVerificationLifetimeAsAnExplicitDurationBean() {
    var configuredLifetime = Duration.ofMinutes(7);

    assertEquals(
        configuredLifetime,
        new ApplicationConfiguration()
            .verificationLifetime(new VerificationProperties(configuredLifetime)));
  }
}
