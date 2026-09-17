package com.incode.verification.adapter.in.web;

import com.incode.verification.application.result.VerificationResult;
import com.incode.verification.domain.provider.ProviderFailure;
import com.incode.verification.domain.provider.ProviderType;
import com.incode.verification.domain.verification.VerificationStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Builder
public record VerificationResponse(
    UUID verificationId,
    String query,
    String normalizedQuery,
    Instant startedAt,
    Instant expiresAt,
    VerificationStatus status,
    @Nullable CompanyResponse company,
    List<CompanyResponse> otherResults,
    @Nullable String provider,
    @Nullable String failure) {
  public VerificationResponse {
    otherResults = copy(otherResults);
  }

  public static VerificationResponse from(VerificationResult result) {
    return VerificationResponse.builder()
        .verificationId(result.id())
        .query(result.rawQuery())
        .normalizedQuery(result.normalizedQuery())
        .startedAt(result.startedAt())
        .expiresAt(result.expiresAt())
        .status(result.status())
        .company(CompanyResponse.from(result.company()))
        .otherResults(result.otherResults().stream().map(CompanyResponse::from).toList())
        .provider(provider(result.provider()))
        .failure(failure(result.failure()))
        .build();
  }

  private static @Nullable String provider(@Nullable ProviderType provider) {
    if (provider == null) {
      return null;
    }
    return provider.name();
  }

  private static @Nullable String failure(@Nullable ProviderFailure failure) {
    if (failure == null) {
      return null;
    }
    return failure.getClass().getSimpleName();
  }

  private static List<CompanyResponse> copy(@Nullable List<CompanyResponse> otherResults) {
    if (otherResults == null) {
      return List.of();
    }
    return List.copyOf(otherResults);
  }
}
