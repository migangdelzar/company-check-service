package com.incode.verification.domain.policy;

import com.incode.verification.domain.type.*;

public final class FallbackPolicy {
  private FallbackPolicy() {}

  public static boolean shouldFallback(ProviderLookupResult result) {
    return result instanceof ProviderLookupResult.Failure failure
        && switch (failure.failure()) {
          case ProviderFailure.Unavailable ignored -> true;
          case ProviderFailure.Malformed ignored -> true;
          case ProviderFailure.ClientError ignored -> false;
          case ProviderFailure.Timeout ignored -> false;
        };
  }
}
