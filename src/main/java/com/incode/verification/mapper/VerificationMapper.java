package com.incode.verification.mapper;

import com.incode.verification.controller.dto.response.CompanyResponse;
import com.incode.verification.controller.dto.response.VerificationResponse;
import com.incode.verification.service.model.ProviderFailure;
import com.incode.verification.service.model.ProviderType;
import com.incode.verification.service.model.VerificationResult;
import org.jspecify.annotations.Nullable;

public final class VerificationMapper {
  private VerificationMapper() {}

  public static VerificationResponse map(VerificationResult result) {
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
    return provider == null ? null : provider.name();
  }

  private static @Nullable String failure(@Nullable ProviderFailure failure) {
    return failure == null ? null : failure.getClass().getSimpleName();
  }
}
