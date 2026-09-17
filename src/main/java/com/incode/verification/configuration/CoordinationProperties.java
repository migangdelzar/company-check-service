package com.incode.verification.configuration;

import com.incode.verification.application.result.VerificationResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("verification.coordination")
@Validated
public record CoordinationProperties(
    @Positive @DefaultValue("10000") int l1MaximumSize,
    @NotNull @DurationMin(inclusive = false) @DefaultValue("10m") Duration ttl,
    @NotNull @DurationMin(inclusive = false) @DefaultValue("24h") Duration matchTtl,
    @NotNull @DurationMin(inclusive = false) @DefaultValue("10m") Duration noMatchTtl,
    @NotNull @DurationMin @DefaultValue("0s") Duration jitter,
    @NotNull @DurationMin(inclusive = false) @DefaultValue("20s") Duration leaseTtl,
    @NotNull @DurationMin(inclusive = false) @DefaultValue("50ms") Duration waiterPoll,
    @PositiveOrZero @DefaultValue("20") int waiterAttempts,
    @NotBlank @DefaultValue("company-check:") String keyPrefix) {

  public Duration ttlFor(VerificationResult result) {
    if (result.company() == null) {
      return noMatchTtl;
    }
    return matchTtl;
  }
}
