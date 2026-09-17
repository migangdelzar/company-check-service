package com.incode.verification.domain.provider;

public final class FallbackPolicy {
  private FallbackPolicy() {}

  public static boolean requiresFallback(ProviderResult result) {
    return result instanceof ProviderResult.Failure failure
        && switch (failure.failure()) {
          case ProviderFailure.Unavailable ignored -> true;
          case ProviderFailure.Malformed ignored -> true;
          case ProviderFailure.ClientError ignored -> false;
          case ProviderFailure.Timeout ignored -> true;
        };
  }
}
