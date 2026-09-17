package com.incode.verification.adapter.out.provider;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Duration;
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
      @NotBlank @Pattern(regexp = "https?://[^\\s]+", message = "must be an HTTP(S) URL")
          String baseUrl,
      @NotBlank @Pattern(regexp = "/[^\\s]*", message = "must be an absolute path template")
          String path,
      @Size(max = 512) String apiKey) {
    @AssertTrue(message = "baseUrl must contain only an HTTP(S) scheme and host")
    public boolean hasSafeBaseUrl() {
      if (baseUrl == null) return false;
      try {
        URI uri = URI.create(baseUrl);
        return uri.getHost() != null
            && uri.getUserInfo() == null
            && uri.getQuery() == null
            && uri.getFragment() == null;
      } catch (IllegalArgumentException exception) {
        return false;
      }
    }
  }

  @AssertTrue(message = "attemptTimeout must be positive")
  public boolean hasPositiveAttemptTimeout() {
    return attemptTimeout != null && !attemptTimeout.isZero() && !attemptTimeout.isNegative();
  }
}
