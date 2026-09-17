package com.incode.verification.adapter.in.web;

import com.incode.verification.application.port.out.VerificationView;
import com.incode.verification.domain.entity.Company;
import com.incode.verification.domain.type.VerificationStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record VerificationResponse(
    UUID verificationId,
    String query,
    String normalizedQuery,
    Instant startedAt,
    Instant expiresAt,
    VerificationStatus status,
    CompanyResponse company,
    List<CompanyResponse> otherResults,
    String provider,
    String failure) {
  public VerificationResponse {
    otherResults = otherResults == null ? List.of() : List.copyOf(otherResults);
  }

  public static VerificationResponse from(VerificationView view) {
    return VerificationResponse.builder()
        .verificationId(view.id())
        .query(view.rawQuery())
        .normalizedQuery(view.normalizedQuery())
        .startedAt(view.startedAt())
        .expiresAt(view.expiresAt())
        .status(view.status())
        .company(CompanyResponse.from(view.company()))
        .otherResults(view.otherResults().stream().map(CompanyResponse::from).toList())
        .provider(view.provider() == null ? null : view.provider().name())
        .failure(view.failure() == null ? null : view.failure().getClass().getSimpleName())
        .build();
  }

  @Builder
  public record CompanyResponse(
      String cin, String name, LocalDate registrationDate, String address, boolean isActive) {
    static CompanyResponse from(Company company) {
      return company == null
          ? null
          : CompanyResponse.builder()
              .cin(company.cin())
              .name(company.name())
              .registrationDate(company.registrationDate())
              .address(company.address())
              .isActive(company.isActive())
              .build();
    }
  }
}
