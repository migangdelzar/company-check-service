package com.incode.verification.adapter.out.persistence;

import com.incode.verification.domain.verification.Verification;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

record VerificationEntity(
    UUID id,
    String rawQuery,
    String normalizedQuery,
    Instant startedAt,
    Instant expiresAt,
    String status,
    String stateJson,
    @Nullable UUID claimToken) {
  static VerificationEntity fromVerification(
      Verification verification, String stateJson, @Nullable UUID claimToken) {
    return new VerificationEntity(
        verification.id(),
        verification.rawQuery(),
        verification.query().value(),
        verification.startedAt(),
        verification.expiresAt(),
        verification.state()
                instanceof com.incode.verification.domain.verification.VerificationState.InProgress
            ? "IN_PROGRESS"
            : verification.state()
                    instanceof
                    com.incode.verification.domain.verification.VerificationState.Completed
                ? "COMPLETED"
                : "FAILED",
        stateJson,
        claimToken);
  }
}
