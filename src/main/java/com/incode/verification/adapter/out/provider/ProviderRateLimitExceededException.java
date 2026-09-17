package com.incode.verification.adapter.out.provider;

final class ProviderRateLimitExceededException extends RuntimeException {
  ProviderRateLimitExceededException() {
    super("provider rate limit exceeded");
  }
}
