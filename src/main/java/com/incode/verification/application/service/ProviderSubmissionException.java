package com.incode.verification.application.service;

import com.incode.verification.domain.type.ProviderFailure;

public final class ProviderSubmissionException extends RuntimeException {
  private final ProviderFailure failure;

  public ProviderSubmissionException(ProviderFailure failure, String message) {
    super(message);
    this.failure = failure;
  }

  public ProviderFailure failure() {
    return failure;
  }

  public int httpStatus() {
    return failure instanceof ProviderFailure.ClientError ? 502 : 503;
  }

  public String code() {
    return failure instanceof ProviderFailure.ClientError
        ? "PROVIDER_CLIENT_ERROR"
        : "PROVIDERS_UNAVAILABLE";
  }

  public String title() {
    return failure instanceof ProviderFailure.ClientError
        ? "Provider client error"
        : "Providers unavailable";
  }
}
