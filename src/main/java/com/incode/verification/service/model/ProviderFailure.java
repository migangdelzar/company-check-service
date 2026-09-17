package com.incode.verification.service.model;

public sealed interface ProviderFailure
    permits ProviderFailure.ClientError,
        ProviderFailure.Unavailable,
        ProviderFailure.Malformed,
        ProviderFailure.Timeout {
  record ClientError(int statusCode) implements ProviderFailure {}

  record Unavailable() implements ProviderFailure {}

  record Malformed() implements ProviderFailure {}

  record Timeout() implements ProviderFailure {}
}
