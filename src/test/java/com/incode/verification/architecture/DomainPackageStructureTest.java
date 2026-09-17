package com.incode.verification.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DomainPackageStructureTest {
  private static final Path DOMAIN_ROOT =
      Path.of("src/main/java/com/incode/verification/domain");

  @Test
  void groupsDomainClassesByBusinessConcept() {
    assertFilesExist(
        "company/Company.java",
        "identity/UuidV7.java",
        "provider/FallbackPolicy.java",
        "provider/ProviderFailure.java",
        "provider/ProviderResult.java",
        "provider/ProviderType.java",
        "query/InvalidQueryException.java",
        "query/NormalizedQuery.java",
        "verification/Verification.java",
        "verification/VerificationState.java",
        "verification/VerificationStatus.java");

    assertDirectoriesAbsent("aggregate", "entity", "policy", "type", "valueobject");
  }

  private void assertFilesExist(String... relativePaths) {
    for (String relativePath : relativePaths) {
      assertTrue(Files.exists(DOMAIN_ROOT.resolve(relativePath)), relativePath);
    }
  }

  private void assertDirectoriesAbsent(String... relativePaths) {
    for (String relativePath : relativePaths) {
      assertFalse(Files.exists(DOMAIN_ROOT.resolve(relativePath)), relativePath);
    }
  }
}
