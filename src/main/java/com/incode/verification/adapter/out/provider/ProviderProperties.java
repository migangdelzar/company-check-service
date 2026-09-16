package com.incode.verification.adapter.out.provider;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("verification.providers")
public record ProviderProperties(Endpoint free, Endpoint premium, Duration attemptTimeout) {
  public ProviderProperties {
    attemptTimeout = attemptTimeout == null ? Duration.ofMillis(500) : attemptTimeout;
  }

  public record Endpoint(String baseUrl, String path, String apiKey) {}
}
