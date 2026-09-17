package com.incode.verification.service.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

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
    if (!expiresAt.isAfter(startedAt)) {
      throw new IllegalArgumentException("expiresAt must be after startedAt");
    }
  }

  public static Verification start(
      UUID id, String rawQuery, NormalizedQuery normalized, Instant now, Instant expiresAt) {
    return new Verification(
        id, rawQuery, normalized, now, expiresAt, new VerificationState.InProgress());
  }

  public Verification complete(ProviderResult.Success result) {
    requireInProgress();
    List<Company> active = result.companies().stream().filter(Company::isActive).toList();
    return new Verification(
        id,
        rawQuery,
        query,
        startedAt,
        expiresAt,
        new VerificationState.Completed(first(active), others(active), result.provider()));
  }

  public Verification apply(ProviderResult result) {
    return switch (result) {
      case ProviderResult.Success success -> complete(success);
      case ProviderResult.Failure failure -> fail(failure.failure());
    };
  }

  public Verification fail(ProviderFailure failure) {
    requireInProgress();
    return new Verification(
        id, rawQuery, query, startedAt, expiresAt, new VerificationState.Failed(failure));
  }

  private void requireInProgress() {
    if (!(state instanceof VerificationState.InProgress)) {
      throw new IllegalStateException("verification is terminal");
    }
  }

  private static @Nullable Company first(List<Company> companies) {
    if (companies.isEmpty()) {
      return null;
    }
    return companies.getFirst();
  }

  private static List<Company> others(List<Company> companies) {
    if (companies.size() < 2) {
      return List.of();
    }
    return companies.subList(1, companies.size());
  }
}
