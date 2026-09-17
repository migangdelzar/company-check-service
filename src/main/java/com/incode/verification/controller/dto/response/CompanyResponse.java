package com.incode.verification.controller.dto.response;

import com.incode.verification.service.model.Company;
import java.time.LocalDate;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Builder
public record CompanyResponse(
    String cin, String name, LocalDate registrationDate, String address, boolean isActive) {
  public static @Nullable CompanyResponse from(@Nullable Company company) {
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
