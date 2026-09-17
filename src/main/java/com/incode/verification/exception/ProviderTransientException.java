package com.incode.verification.exception;

import com.incode.verification.service.model.ProviderFailure;
import org.jspecify.annotations.Nullable;

public final class ProviderTransientException extends RuntimeException {
  private final ProviderFailure failure;

  public ProviderTransientException(ProviderFailure failure, @Nullable Throwable cause) {
    super(cause);
    this.failure = failure;
  }

  public ProviderTransientException(ProviderFailure failure) {
    this(failure, null);
  }

  public ProviderFailure failure() {
    return failure;
  }
}
