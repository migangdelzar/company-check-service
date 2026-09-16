package com.incode.verification.adapter.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("verification.coordination")
public record CoordinationProperties(
    int l1MaximumSize,
    Duration ttl,
    Duration jitter,
    Duration leaseTtl,
    Duration waiterPoll,
    int waiterAttempts,
    String keyPrefix) {
  public CoordinationProperties {
    if (l1MaximumSize < 1
        || ttl.isNegative()
        || jitter.isNegative()
        || leaseTtl.isNegative()
        || waiterPoll.isNegative()
        || waiterAttempts < 0)
      throw new IllegalArgumentException("invalid coordination configuration");
    keyPrefix = keyPrefix == null || keyPrefix.isBlank() ? "company-check:" : keyPrefix;
  }
}
