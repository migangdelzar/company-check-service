package com.incode.verification.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class LayerPackageStructureTest {
  private static final Path SOURCE_ROOT = Path.of("src/main/java/com/incode/verification");

  @Test
  void containsOnlyTheApprovedLayerRoots() {
    for (String layer :
        new String[] {
          "controller", "service", "repository", "client", "mapper", "exception", "config"
        }) {
      assertTrue(Files.isDirectory(SOURCE_ROOT.resolve(layer)), layer);
    }
    for (String obsolete : new String[] {"adapter", "application", "domain"}) {
      assertFalse(Files.exists(SOURCE_ROOT.resolve(obsolete)), obsolete);
    }
  }

  @Test
  void keepsBoundaryDtosAndPersistenceEntitiesInsideTheirLayers() {
    assertTrue(Files.isDirectory(SOURCE_ROOT.resolve("client/dto")));
    assertTrue(Files.isDirectory(SOURCE_ROOT.resolve("repository/entity")));
  }
}
