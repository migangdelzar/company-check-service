package com.incode.verification.configuration.persistence;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("verification.database")
@Validated
public record DatabaseProperties(
    @NotNull @DurationMin(inclusive = false) @DefaultValue("5s") Duration transactionTimeout) {}
