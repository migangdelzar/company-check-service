package com.incode.verification.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AdapterPackageStructureTest {
  private static final Path SOURCE_ROOT = Path.of("src/main/java/com/incode/verification");

  @Test
  void separatesConfigurationInboundSchedulingAndProviderAdapters() {
    assertTrue(
        Files.exists(
            SOURCE_ROOT.resolve("configuration/application/ApplicationConfiguration.java")));
    assertTrue(
        Files.exists(
            SOURCE_ROOT.resolve("configuration/provider/ProviderResilienceConfiguration.java")));
    assertTrue(
        Files.exists(
            SOURCE_ROOT.resolve("configuration/persistence/PersistenceConfiguration.java")));
    assertTrue(
        Files.exists(
            SOURCE_ROOT.resolve("configuration/coordination/CoordinationConfiguration.java")));
    assertTrue(
        Files.exists(SOURCE_ROOT.resolve("configuration/web/InboundRateLimitConfiguration.java")));
    assertTrue(
        Files.exists(
            SOURCE_ROOT.resolve("adapter/in/scheduling/VerificationExpirationScheduler.java")));
    assertTrue(Files.exists(SOURCE_ROOT.resolve("adapter/out/provider/FreeProvider.java")));
    assertTrue(Files.exists(SOURCE_ROOT.resolve("adapter/out/provider/ResilientProvider.java")));
    assertTrue(
        Files.exists(SOURCE_ROOT.resolve("application/exception/VerificationException.java")));
    assertTrue(
        Files.exists(
            SOURCE_ROOT.resolve("application/exception/VerificationConflictException.java")));
    assertTrue(
        Files.exists(
            SOURCE_ROOT.resolve("application/exception/VerificationNotFoundException.java")));
    assertTrue(
        Files.exists(
            SOURCE_ROOT.resolve("application/exception/ProviderSubmissionException.java")));
    assertTrue(
        Files.exists(
            SOURCE_ROOT.resolve("application/exception/CoordinationUnavailableException.java")));

    assertFalse(Files.exists(SOURCE_ROOT.resolve("adapter/config")));
    assertFalse(Files.exists(SOURCE_ROOT.resolve("adapter/out/expiration")));
    assertFalse(Files.exists(SOURCE_ROOT.resolve("configuration/ApplicationConfiguration.java")));
    assertFalse(
        Files.exists(SOURCE_ROOT.resolve("configuration/ProviderResilienceConfiguration.java")));
    assertFalse(
        Files.exists(SOURCE_ROOT.resolve("application/service/VerificationException.java")));
    assertFalse(
        Files.exists(
            SOURCE_ROOT.resolve("application/service/VerificationConflictException.java")));
    assertFalse(
        Files.exists(
            SOURCE_ROOT.resolve("application/service/VerificationNotFoundException.java")));
    assertFalse(
        Files.exists(SOURCE_ROOT.resolve("application/service/ProviderSubmissionException.java")));
    assertFalse(
        Files.exists(
            SOURCE_ROOT.resolve("application/service/CoordinationUnavailableException.java")));
  }
}
