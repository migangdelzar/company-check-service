package com.incode.verification.adapter.config;

import com.incode.verification.application.port.out.VerificationView;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("verification.coordination")
public record CoordinationProperties(
    int l1MaximumSize,
    Duration ttl,
    Duration matchTtl,
    Duration noMatchTtl,
    Duration jitter,
    Duration leaseTtl,
    Duration waiterPoll,
    int waiterAttempts,
    String keyPrefix) {
  public CoordinationProperties {
    if (l1MaximumSize < 1
        || ttl.isNegative()
        || (matchTtl != null && matchTtl.isNegative())
        || (noMatchTtl != null && noMatchTtl.isNegative())
        || jitter.isNegative()
        || leaseTtl.isNegative()
        || waiterPoll.isNegative()
        || waiterAttempts < 0)
      throw new IllegalArgumentException("invalid coordination configuration");
    keyPrefix = keyPrefix == null || keyPrefix.isBlank() ? "company-check:" : keyPrefix;
    matchTtl = matchTtl == null ? Duration.ofHours(24) : matchTtl;
    noMatchTtl = noMatchTtl == null ? ttl : noMatchTtl;
  }

  public Duration ttlFor(VerificationView view) {
    return view.company() == null ? noMatchTtl : matchTtl;
  }
}
