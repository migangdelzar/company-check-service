package com.incode.verification.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("verification.expiration.lock")
@Validated
public record ExpirationLockProperties(
    @NotBlank @DefaultValue("company-check:expiration:lock") String key,
    @NotNull @DurationMin(inclusive = false) @DefaultValue("30s") Duration ttl) {}
