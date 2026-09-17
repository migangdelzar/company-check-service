package com.incode.verification.configuration.persistence;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("verification.database.retry")
@Validated
public record DatabaseRetryProperties(
    @Positive @DefaultValue("3") int maxAttempts,
    @NotNull @DurationMin @DefaultValue("50ms") Duration delay,
    @Positive @DefaultValue("2") double multiplier,
    @NotNull @DurationMin @DefaultValue("500ms") Duration maxDelay) {}
