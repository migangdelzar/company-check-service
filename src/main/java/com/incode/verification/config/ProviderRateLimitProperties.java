package com.incode.verification.config;

import com.incode.verification.service.model.ProviderType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("verification.rate-limiting")
@Validated
public record ProviderRateLimitProperties(
    @NotNull @Valid Limit free, @NotNull @Valid Limit premium, @NotBlank String keyPrefix) {
  public Limit forProvider(ProviderType provider) {
    return switch (provider) {
      case FREE -> free;
      case PREMIUM -> premium;
    };
  }

  public record Limit(
      @Positive int limitForPeriod,
      @NotNull @DurationMin(inclusive = false) Duration refreshPeriod) {}
}
