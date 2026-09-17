package com.incode.verification.adapter.out.provider;

import com.incode.verification.domain.provider.ProviderFailure;
import org.jspecify.annotations.Nullable;

public final class ProviderTransientException extends RuntimeException {
  private final ProviderFailure failure;

  ProviderTransientException(ProviderFailure failure, @Nullable Throwable cause) {
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
