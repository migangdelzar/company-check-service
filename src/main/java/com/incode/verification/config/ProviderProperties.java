package com.incode.verification.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("verification.providers")
@Validated
public record ProviderProperties(
    @NotNull @Valid ProviderEndpointProperties free,
    @NotNull @Valid ProviderEndpointProperties premium,
    @NotNull @DurationMin(inclusive = false) @DefaultValue("500ms") Duration attemptTimeout,
    @NotNull @Valid @DefaultValue HttpPoolProperties pool) {
  @ConstructorBinding
  public ProviderProperties(
      ProviderEndpointProperties free,
      ProviderEndpointProperties premium,
      Duration attemptTimeout,
      HttpPoolProperties pool) {
    this.free = free;
    this.premium = premium;
    this.attemptTimeout = attemptTimeout;
    this.pool = pool == null ? new HttpPoolProperties(100, 50) : pool;
  }

  public ProviderProperties(
      ProviderEndpointProperties free,
      ProviderEndpointProperties premium,
      Duration attemptTimeout) {
    this(free, premium, attemptTimeout, new HttpPoolProperties(100, 50));
  }

  public record HttpPoolProperties(
      @Positive @DefaultValue("100") int maxConnectionsTotal,
      @Positive @DefaultValue("50") int maxConnectionsPerRoute,
      @NotNull @DurationMin(inclusive = false) @DefaultValue("100ms")
          Duration connectionRequestTimeout,
      @NotNull @DurationMin(inclusive = false) @DefaultValue("150ms") Duration connectTimeout,
      @NotNull @DurationMin(inclusive = false) @DefaultValue("400ms") Duration responseTimeout,
      @NotNull @DurationMin(inclusive = false) @DefaultValue("5s") Duration validateAfterInactivity,
      @NotNull @DurationMin(inclusive = false) @DefaultValue("30s") Duration evictIdleAfter) {
    public HttpPoolProperties(int maxConnectionsTotal, int maxConnectionsPerRoute) {
      this(
          maxConnectionsTotal,
          maxConnectionsPerRoute,
          Duration.ofMillis(100),
          Duration.ofMillis(150),
          Duration.ofMillis(400),
          Duration.ofSeconds(5),
          Duration.ofSeconds(30));
    }
  }
}
