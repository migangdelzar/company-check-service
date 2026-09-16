package com.incode.verification.configuration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.incode.verification.adapter.config.ApplicationConfiguration;
import com.incode.verification.adapter.out.expiration.VerificationExpirationScheduler;
import java.nio.file.Files;
import java.nio.file.Path;
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

  @Test
  void approvedSpringBaselineIsDeclaredInVersionCatalog() throws Exception {
    String catalog = Files.readString(Path.of("gradle/libs.versions.toml"));
    String build = Files.readString(Path.of("build.gradle.kts"));

    assertTrue(catalog.contains("spring-boot = \"4.1.1\""));
    assertTrue(catalog.contains("spring-modulith = \"2.1.1\""));
    assertTrue(catalog.contains("resilience4j = \"2.4.0\""));
    assertTrue(catalog.contains("resilience4j-spring-boot4"));
    assertTrue(build.contains("JavaLanguageVersion.of(25)"));
    assertFalse(catalog.contains("resilience4j-spring-boot3"));
  }
}
