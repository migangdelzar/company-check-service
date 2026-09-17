package com.incode.verification.repository.entity;

import com.incode.verification.service.model.Verification;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record VerificationEntity(
    UUID id,
    String rawQuery,
    String normalizedQuery,
    Instant startedAt,
    Instant expiresAt,
    String status,
    String stateJson,
    @Nullable UUID claimToken) {
  public static VerificationEntity fromVerification(
      Verification verification, String stateJson, @Nullable UUID claimToken) {
    return new VerificationEntity(
        verification.id(),
        verification.rawQuery(),
        verification.query().value(),
        verification.startedAt(),
        verification.expiresAt(),
        verification.state()
                instanceof com.incode.verification.service.model.VerificationState.InProgress
            ? "IN_PROGRESS"
            : verification.state()
                    instanceof com.incode.verification.service.model.VerificationState.Completed
                ? "COMPLETED"
                : "FAILED",
        stateJson,
        claimToken);
  }
}
