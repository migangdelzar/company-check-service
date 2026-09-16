package com.incode.verification.adapter.config;

import com.incode.verification.application.port.out.VerificationView;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("verification.coordination")
@Validated
public record CoordinationProperties(
    @Min(1) int l1MaximumSize,
    @NotNull Duration ttl,
    Duration matchTtl,
    Duration noMatchTtl,
    @NotNull Duration jitter,
    @NotNull Duration leaseTtl,
    @NotNull Duration waiterPoll,
    @Min(0) int waiterAttempts,
    String keyPrefix) {
  public CoordinationProperties {
    ttl = ttl == null ? Duration.ofMinutes(10) : ttl;
    jitter = jitter == null ? Duration.ZERO : jitter;
    leaseTtl = leaseTtl == null ? Duration.ofSeconds(20) : leaseTtl;
    waiterPoll = waiterPoll == null ? Duration.ofMillis(50) : waiterPoll;
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
