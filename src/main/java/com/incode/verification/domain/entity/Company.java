package com.incode.verification.domain.entity;

import java.time.LocalDate;
import java.util.Objects;

public record Company(
    String cin, String name, LocalDate registrationDate, String address, boolean isActive) {
  public Company(
      String cin, String name, LocalDate registrationDate, String address, boolean isActive) {
    this.cin = Objects.requireNonNull(cin, "cin");
    this.name = Objects.requireNonNull(name, "name");
    this.registrationDate = Objects.requireNonNull(registrationDate, "registrationDate");
    this.address = Objects.requireNonNull(address, "address");
    this.isActive = isActive;
  }
}
