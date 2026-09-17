package com.incode.verification.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.incode.verification.service.model.Company;
import java.time.LocalDate;

public record FreeCompanyResponse(
    String cin,
    String name,
    @JsonProperty("registration_date") String registrationDate,
    String address,
    @JsonProperty("is_active") Boolean active) {
  public Company toCompany() {
    if (cin == null
        || name == null
        || registrationDate == null
        || address == null
        || active == null) {
      throw malformed();
    }
    try {
      return new Company(cin, name, LocalDate.parse(registrationDate), address, active);
    } catch (RuntimeException exception) {
      throw malformed(exception);
    }
  }

  private static IllegalArgumentException malformed() {
    return new IllegalArgumentException("free provider company is malformed");
  }

  private static IllegalArgumentException malformed(Throwable cause) {
    return new IllegalArgumentException("free provider company is malformed", cause);
  }
}
