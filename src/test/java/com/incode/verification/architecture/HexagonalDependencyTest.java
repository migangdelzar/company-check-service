package com.incode.verification.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.incode.verification.domain.verification.Verification;
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

  @Test
  void expirationReclaimsExpiredRowsEvenWhenAStaleClaimRemains() throws Exception {
    String repository =
        Files.readString(
            Path.of(
                "src/main/java/com/incode/verification/adapter/out/persistence/JdbcVerificationRepository.java"));

    assertTrue(repository.contains("AND expires_at<=:now ORDER BY expires_at"));
    assertFalse(repository.contains("AND expires_at<=:now AND claim_token IS NULL"));
    assertTrue(repository.contains("claim_token=NULL,claimed_at=NULL"));
  }
}
