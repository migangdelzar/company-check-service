package com.incode.verification.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.incode.verification.controller.VerificationExpirationScheduler;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;

class IncodeCompositionTest {
  @Test
  void compositionIsExplicitAndSchedulingIsControllerOwned() {
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
    assertTrue(catalog.contains("resilience4j = \"2.4.0\""));
    assertTrue(catalog.contains("resilience4j-spring-boot4"));
    assertTrue(catalog.contains("java = \"25\""));
    assertTrue(build.contains("java.toolchain.languageVersion"));
    assertFalse(catalog.contains("resilience4j-spring-boot3"));
    assertFalse(catalog.contains("spring-modulith"));
    assertFalse(build.contains("spring.modulith"));
  }

  @Test
  void nativeBuildUsesAnAutomaticallyProvisionedNativeImageToolchain() throws Exception {
    String settings = Files.readString(Path.of("settings.gradle.kts"));
    String build = Files.readString(Path.of("build.gradle.kts"));

    assertTrue(settings.contains("org.gradle.toolchains.foojay-resolver-convention"));
    assertTrue(build.contains("graalvmNative"));
    assertTrue(build.contains("toolchainDetection.set(true)"));
    assertTrue(build.contains("nativeImageCapable.set(true)"));
    assertTrue(build.contains("javaLauncher.set"));
  }
}
