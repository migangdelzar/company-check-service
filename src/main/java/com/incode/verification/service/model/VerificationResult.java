package com.incode.verification.service.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Builder
public record VerificationResult(
    UUID id,
    String rawQuery,
    String normalizedQuery,
    Instant startedAt,
    Instant expiresAt,
    VerificationStatus status,
    @Nullable Company company,
    List<Company> otherResults,
    @Nullable ProviderType provider,
    @Nullable ProviderFailure failure) {
  public static VerificationResult from(Verification verification) {
    var builder =
        builder()
            .id(verification.id())
            .rawQuery(verification.rawQuery())
            .normalizedQuery(verification.query().value())
            .startedAt(verification.startedAt())
            .expiresAt(verification.expiresAt());
    return switch (verification.state()) {
      case VerificationState.InProgress ignored ->
          builder.status(VerificationStatus.IN_PROGRESS).build();
      case VerificationState.Completed completed ->
          builder
              .status(VerificationStatus.COMPLETED)
              .company(completed.company())
              .otherResults(completed.otherResults())
              .provider(completed.provider())
              .build();
      case VerificationState.Failed failed ->
          builder.status(VerificationStatus.FAILED).failure(failed.failure()).build();
    };
  }

  public VerificationResult {
    otherResults = copy(otherResults);
  }

  private static List<Company> copy(@Nullable List<Company> otherResults) {
    if (otherResults == null) {
      return List.of();
    }
    return List.copyOf(otherResults);
  }
}
