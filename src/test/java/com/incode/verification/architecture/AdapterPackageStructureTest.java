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
    assertTrue(Files.exists(SOURCE_ROOT.resolve("configuration/ApplicationConfiguration.java")));
    assertTrue(
        Files.exists(SOURCE_ROOT.resolve("configuration/ProviderResilienceConfiguration.java")));
    assertTrue(
        Files.exists(
            SOURCE_ROOT.resolve("adapter/in/scheduling/VerificationExpirationScheduler.java")));
    assertTrue(Files.exists(SOURCE_ROOT.resolve("adapter/out/provider/FreeProvider.java")));
    assertTrue(Files.exists(SOURCE_ROOT.resolve("adapter/out/provider/ResilientProvider.java")));

    assertFalse(Files.exists(SOURCE_ROOT.resolve("adapter/config")));
    assertFalse(Files.exists(SOURCE_ROOT.resolve("adapter/out/expiration")));
  }
}
