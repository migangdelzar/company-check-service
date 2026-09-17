package com.incode.verification.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("verification.inbound-rate-limiting")
@Validated
public record InboundRateLimitProperties(
    @Positive int limitForPeriod,
    @NotNull @DurationMin(inclusive = false) Duration refreshPeriod,
    @NotBlank String keyPrefix) {}
