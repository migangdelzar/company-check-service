package com.incode.verification.mapper;

import com.incode.verification.service.model.Company;
import com.incode.verification.service.model.ProviderResult;
import com.incode.verification.service.model.ProviderType;
import com.incode.verification.service.model.Verification;
import com.incode.verification.service.model.VerificationResult;
import com.incode.verification.service.model.VerificationState;
import com.incode.verification.service.model.VerificationStatus;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

public final class VerificationReconciliationMapper {
  private VerificationReconciliationMapper() {}

  public static Verification fromCached(Verification verification, VerificationResult result) {
    if (result.status() == VerificationStatus.FAILED) {
      return verification.fail(Objects.requireNonNull(result.failure()));
    }
    return verification.complete(
        new ProviderResult.Success(
            companies(result.company(), result.otherResults()), provider(result.provider())));
  }

  public static Verification fromShared(Verification verification, Verification shared) {
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
