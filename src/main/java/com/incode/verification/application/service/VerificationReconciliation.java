package com.incode.verification.application.service;

import com.incode.verification.application.result.VerificationResult;
import com.incode.verification.domain.company.Company;
import com.incode.verification.domain.provider.ProviderResult;
import com.incode.verification.domain.provider.ProviderType;
import com.incode.verification.domain.verification.Verification;
import com.incode.verification.domain.verification.VerificationState;
import com.incode.verification.domain.verification.VerificationStatus;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

final class VerificationReconciliation {
  private VerificationReconciliation() {}

  static Verification fromCached(Verification verification, VerificationResult result) {
    if (result.status() == VerificationStatus.FAILED) {
      return verification.fail(Objects.requireNonNull(result.failure()));
    }
    return verification.complete(
        new ProviderResult.Success(
            companies(result.company(), result.otherResults()), provider(result.provider())));
  }

  static Verification fromShared(Verification verification, Verification shared) {
    return switch (shared.state()) {
      case VerificationState.InProgress ignored -> verification;
      case VerificationState.Completed completed ->
          verification.complete(
              new ProviderResult.Success(
                  companies(completed.company(), completed.otherResults()), completed.provider()));
      case VerificationState.Failed failed -> verification.fail(failed.failure());
    };
  }

  private static List<Company> companies(@Nullable Company company, List<Company> otherResults) {
    return Stream.concat(Stream.ofNullable(company), otherResults.stream()).toList();
  }

  private static ProviderType provider(@Nullable ProviderType provider) {
    if (provider == null) {
      return ProviderType.FREE;
    }
    return provider;
  }
}
