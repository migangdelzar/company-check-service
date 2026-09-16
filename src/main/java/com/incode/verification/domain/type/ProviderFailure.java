package com.incode.verification.domain.type;

public sealed interface ProviderFailure permits ProviderFailure.ClientError, ProviderFailure.Unavailable,
        ProviderFailure.Malformed, ProviderFailure.Timeout {
    record ClientError(int statusCode) implements ProviderFailure { }
    record Unavailable() implements ProviderFailure { }
    record Malformed() implements ProviderFailure { }
    record Timeout() implements ProviderFailure { }
}
