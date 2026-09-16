package com.incode.verification.adapter.out.persistence;

import com.incode.verification.domain.aggregate.Verification;
import java.time.Instant;
import java.util.UUID;

record VerificationEntity(
    UUID id,
    String rawQuery,
    String normalizedQuery,
    Instant startedAt,
    Instant expiresAt,
    String status,
    String stateJson,
    UUID claimToken) {
  static VerificationEntity from(Verification v, String json, UUID token) {
    return new VerificationEntity(
        v.id(),
        v.rawQuery(),
        v.query().value(),
        v.startedAt(),
        v.expiresAt(),
        v.state() instanceof com.incode.verification.domain.type.VerificationState.InProgress
            ? "IN_PROGRESS"
            : v.state() instanceof com.incode.verification.domain.type.VerificationState.Completed
                ? "COMPLETED"
                : "FAILED",
        json,
        token);
  }
}
