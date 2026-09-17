package com.incode.verification.configuration.provider;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.net.URI;

public record ProviderEndpointProperties(
    @NotBlank @Pattern(regexp = "https?://[^\\s]+", message = "must be an HTTP(S) URL")
        String baseUrl,
    @NotBlank @Pattern(regexp = "/[^\\s]*", message = "must be an absolute path template")
        String path,
    @Size(max = 512) String apiKey) {
  @AssertTrue(message = "baseUrl must contain only an HTTP(S) scheme and host")
  public boolean hasSafeBaseUrl() {
    if (baseUrl == null) {
      return false;
    }
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
