package com.incode.verification.adapter.out.provider;

import com.incode.verification.domain.type.ProviderFailure;

public final class ProviderTransientException extends RuntimeException {
  private final ProviderFailure failure;

  ProviderTransientException(ProviderFailure failure, Throwable cause) {
    super(cause);
    this.failure = failure;
  }

  ProviderTransientException(ProviderFailure failure) {
    this(failure, null);
  }

  public ProviderFailure failure() {
    return failure;
  }
}
