package com.incode.verification.domain.provider;

import com.incode.verification.domain.company.Company;
import java.util.List;
import java.util.Objects;

public sealed interface ProviderResult
    permits ProviderResult.Success, ProviderResult.Failure {
  record Success(List<Company> companies, ProviderType provider) implements ProviderResult {
    public Success(List<Company> companies) {
      this(companies, ProviderType.FREE);
    }

    public Success {
      companies = List.copyOf(Objects.requireNonNull(companies, "companies"));
    }
  }

  record Failure(ProviderFailure failure) implements ProviderResult {
    public Failure {
      Objects.requireNonNull(failure, "failure");
    }
  }
}
