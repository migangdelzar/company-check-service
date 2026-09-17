package com.incode.verification.controller.dto.response;

import com.incode.verification.service.model.VerificationStatus;
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

  private static List<CompanyResponse> copy(@Nullable List<CompanyResponse> otherResults) {
    if (otherResults == null) {
      return List.of();
    }
    return List.copyOf(otherResults);
  }
}
