package com.incode.verification.adapter.out.provider;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.Validation;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class ProviderPropertiesTest {
  @Test
  void invalidProviderConfigurationIsRejectedByValidation() {
    var invalid =
        new ProviderProperties(
            new ProviderEndpointProperties("not-a-url", " ", ""),
            new ProviderEndpointProperties("http://premium", "/lookup", ""),
            Duration.ofMillis(-1));

    assertFalse(
        Validation.buildDefaultValidatorFactory().getValidator().validate(invalid).isEmpty());
  }

  @Test
  void validProviderConfigurationPassesValidation() {
    var valid =
        new ProviderProperties(
            new ProviderEndpointProperties("http://free", "/lookup", ""),
            new ProviderEndpointProperties("http://premium", "/lookup", ""),
            Duration.ofMillis(500));

    assertTrue(Validation.buildDefaultValidatorFactory().getValidator().validate(valid).isEmpty());
  }

  @Test
  void invalidProviderPoolConfigurationIsRejectedByValidation() {
    var invalid =
        new ProviderProperties(
            new ProviderEndpointProperties("http://free", "/lookup", ""),
            new ProviderEndpointProperties("http://premium", "/lookup", ""),
            Duration.ofMillis(500),
            new ProviderProperties.HttpPoolProperties(0, -1));

    assertFalse(
        Validation.buildDefaultValidatorFactory().getValidator().validate(invalid).isEmpty());
  }

  @Test
  void validProviderPoolConfigurationPassesValidation() {
    var valid =
        new ProviderProperties(
            new ProviderEndpointProperties("http://free", "/lookup", ""),
            new ProviderEndpointProperties("http://premium", "/lookup", ""),
            Duration.ofMillis(500),
            new ProviderProperties.HttpPoolProperties(100, 100));

    assertTrue(Validation.buildDefaultValidatorFactory().getValidator().validate(valid).isEmpty());
  }

  @Test
  void explicitProviderPoolTimeoutsAreAvailable() {
    var pool =
        new ProviderProperties.HttpPoolProperties(
            100,
            50,
            Duration.ofMillis(100),
            Duration.ofMillis(150),
            Duration.ofMillis(400),
            Duration.ofSeconds(5),
            Duration.ofSeconds(30));

    assertEquals(Duration.ofMillis(100), pool.connectionRequestTimeout());
    assertEquals(Duration.ofMillis(150), pool.connectTimeout());
    assertEquals(Duration.ofMillis(400), pool.responseTimeout());
    assertEquals(Duration.ofSeconds(5), pool.validateAfterInactivity());
    assertEquals(Duration.ofSeconds(30), pool.evictIdleAfter());
  }
}
