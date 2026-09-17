package com.incode.verification.exception;

import com.incode.verification.service.model.ProviderFailure;

public final class ProviderSubmissionException extends IntegrationException {
  private final ProviderFailure failure;

  public ProviderSubmissionException(ProviderFailure failure, String message) {
    super(status(failure), title(failure), code(failure), message);
    this.failure = failure;
  }

  public ProviderFailure failure() {
    return failure;
  }

  private static int status(ProviderFailure failure) {
    if (failure instanceof ProviderFailure.ClientError) {
      return 502;
    }
    return 503;
  }

  private static String code(ProviderFailure failure) {
    return failure instanceof ProviderFailure.ClientError
        ? "PROVIDER_CLIENT_ERROR"
        : "PROVIDERS_UNAVAILABLE";
  }

  private static String title(ProviderFailure failure) {
    return failure instanceof ProviderFailure.ClientError
        ? "Provider client error"
        : "Providers unavailable";
  }
}
