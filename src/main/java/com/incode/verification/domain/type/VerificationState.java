package com.incode.verification.domain.type;

import com.incode.verification.domain.entity.Company;
import java.util.List;

public sealed interface VerificationState
    permits VerificationState.InProgress, VerificationState.Completed, VerificationState.Failed {
  record InProgress() implements VerificationState {}

  record Completed(Company company, List<Company> otherResults, ProviderType provider)
      implements VerificationState {
    public Completed {
      otherResults = List.copyOf(otherResults);
    }
  }

  record Failed(ProviderFailure failure) implements VerificationState {}
}
