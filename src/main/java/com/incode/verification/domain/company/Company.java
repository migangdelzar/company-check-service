package com.incode.verification.domain.company;

import java.time.LocalDate;
import java.util.Objects;

public record Company(
    String cin, String name, LocalDate registrationDate, String address, boolean isActive) {
  public Company(
      String cin, String name, LocalDate registrationDate, String address, boolean isActive) {
    this.cin = requiredText(cin, "cin");
    this.name = requiredText(name, "name");
    this.registrationDate = Objects.requireNonNull(registrationDate, "registrationDate");
    this.address = requiredText(address, "address");
    this.isActive = isActive;
  }

  private static String requiredText(String value, String field) {
    String required = Objects.requireNonNull(value, field);
    if (required.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return required;
  }
}
