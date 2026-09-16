package com.incode.verification.adapter.out.provider;

import java.time.Duration;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.AssertTrue;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("verification.providers")
@Validated
public record ProviderProperties(
    @NotNull @Valid Endpoint free,
    @NotNull @Valid Endpoint premium,
    @NotNull Duration attemptTimeout) {
  public ProviderProperties {
    attemptTimeout = attemptTimeout == null ? Duration.ofMillis(500) : attemptTimeout;
  }

  public record Endpoint(
      @NotBlank
          @Pattern(regexp = "https?://[^\\s]+", message = "must be an HTTP(S) URL")
          String baseUrl,
      @NotBlank String path,
      String apiKey) {}

  @AssertTrue(message = "attemptTimeout must be positive")
  public boolean hasPositiveAttemptTimeout() {
    return attemptTimeout != null && !attemptTimeout.isZero() && !attemptTimeout.isNegative();
  }
}
