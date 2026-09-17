package com.incode.verification.domain.aggregate;

import com.incode.verification.domain.entity.Company;
import com.incode.verification.domain.type.ProviderFailure;
import com.incode.verification.domain.type.ProviderLookupResult;
import com.incode.verification.domain.type.VerificationState;
import com.incode.verification.domain.valueobject.NormalizedQuery;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record Verification(
    UUID id,
    String rawQuery,
    NormalizedQuery query,
    Instant startedAt,
    Instant expiresAt,
    VerificationState state) {
  public Verification {
    Objects.requireNonNull(id);
    Objects.requireNonNull(rawQuery);
    Objects.requireNonNull(query);
    Objects.requireNonNull(startedAt);
    Objects.requireNonNull(expiresAt);
    Objects.requireNonNull(state);
    if (!expiresAt.isAfter(startedAt))
      throw new IllegalArgumentException("expiresAt must be after startedAt");
  }

  public static Verification start(
      UUID id, String rawQuery, NormalizedQuery normalized, Instant now, Instant expiresAt) {
    return new Verification(
        id, rawQuery, normalized, now, expiresAt, new VerificationState.InProgress());
  }

  public Verification complete(ProviderLookupResult.Success result) {
    requireInProgress();
    List<Company> active = result.companies().stream().filter(Company::isActive).toList();
    return new Verification(
        id,
        rawQuery,
        query,
        startedAt,
        expiresAt,
        new VerificationState.Completed(
            active.isEmpty() ? null : active.getFirst(),
            active.size() < 2 ? List.of() : active.subList(1, active.size()),
            result.provider()));
  }

  public Verification complete(ProviderLookupResult.Success result, Instant ignored) {
    return complete(result);
  }

  public Verification fail(ProviderFailure failure) {
    requireInProgress();
    return new Verification(
        id, rawQuery, query, startedAt, expiresAt, new VerificationState.Failed(failure));
  }

  public Verification fail(ProviderFailure failure, Instant ignored) {
    return fail(failure);
  }

  private void requireInProgress() {
    if (!(state instanceof VerificationState.InProgress))
      throw new IllegalStateException("verification is terminal");
  }
}
