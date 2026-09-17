package com.incode.verification.exception;

public final class ProviderRateLimitExceededException extends RuntimeException {
  public ProviderRateLimitExceededException() {
    super("provider rate limit exceeded");
  }
}
