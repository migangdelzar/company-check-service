package com.incode.verification.configuration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.incode.verification.adapter.config.ApplicationConfiguration;
import com.incode.verification.adapter.out.expiration.VerificationExpirationScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;

class IncodeCompositionTest {
  @Test
  void compositionIsExplicitAndSchedulingIsAdapterOwned() {
    assertFalse(
        Configuration.class
            .cast(ApplicationConfiguration.class.getAnnotation(Configuration.class))
            .proxyBeanMethods());
    assertTrue(
        VerificationExpirationScheduler.class.isAnnotationPresent(
            org.springframework.stereotype.Component.class));
  }
}
