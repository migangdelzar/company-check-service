package com.incode.verification.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.incode.verification.service.model.Company;
import java.time.LocalDate;

public record PremiumCompanyResponse(
    String companyIdentificationNumber,
    String companyName,
    String registrationDate,
    String companyFullAddress,
    @JsonProperty("isActive") Boolean active) {
  public Company toCompany() {
    if (companyIdentificationNumber == null
        || companyName == null
        || registrationDate == null
        || companyFullAddress == null
        || active == null) {
      throw malformed();
    }
    try {
      return new Company(
          companyIdentificationNumber,
          companyName,
          LocalDate.parse(registrationDate),
          companyFullAddress,
          active);
    } catch (RuntimeException exception) {
      throw malformed(exception);
    }
  }

  private static IllegalArgumentException malformed() {
    return new IllegalArgumentException("premium provider company is malformed");
  }

  private static IllegalArgumentException malformed(Throwable cause) {
    return new IllegalArgumentException("premium provider company is malformed", cause);
  }
}
