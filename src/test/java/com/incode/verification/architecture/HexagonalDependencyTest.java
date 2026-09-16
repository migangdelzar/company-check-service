package com.incode.verification.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.incode.verification.domain.aggregate.Verification;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class HexagonalDependencyTest {
  @Test
  void domainTypesHaveNoSpringAnnotations() {
    assertFalse(
        Verification.class.isAnnotationPresent(org.springframework.stereotype.Component.class));
  }

  @Test
  void serviceRulesRequireExplicitVerificationColumnsAndGuidance() throws Exception {
    String repository =
        Files.readString(
            Path.of(
                "src/main/java/com/incode/verification/adapter/out/persistence/JdbcVerificationRepository.java"));
    String guidance = Files.readString(Path.of("AGENTS.md"));

    assertTrue(
        repository.contains("SELECT id,raw_query,normalized_query,started_at,expires_at,state"));
    assertTrue(guidance.contains("JDBC"));
    assertTrue(guidance.contains("JPA"));
    assertTrue(guidance.contains("framework-free"));
    assertTrue(guidance.contains("exception"));
  }
}
