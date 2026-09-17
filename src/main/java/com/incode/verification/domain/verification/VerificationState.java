package com.incode.verification.domain.verification;

import com.incode.verification.domain.company.Company;
import com.incode.verification.domain.provider.ProviderFailure;
import com.incode.verification.domain.provider.ProviderType;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public sealed interface VerificationState
    permits VerificationState.InProgress, VerificationState.Completed, VerificationState.Failed {
  record InProgress() implements VerificationState {}

  record Completed(@Nullable Company company, List<Company> otherResults, ProviderType provider)
      implements VerificationState {
    public Completed {
      otherResults = List.copyOf(Objects.requireNonNull(otherResults, "otherResults"));
      Objects.requireNonNull(provider, "provider");
    }
  }

  record Failed(ProviderFailure failure) implements VerificationState {}
}
