package com.incode.verification.adapter.out.provider;

import static org.junit.jupiter.api.Assertions.assertFalse;

import jakarta.validation.Validation;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class ProviderPropertiesTest {
  @Test
  void invalidProviderConfigurationIsRejectedByValidation() {
    var invalid =
        new ProviderProperties(
            new ProviderProperties.Endpoint("not-a-url", " ", ""),
            new ProviderProperties.Endpoint("http://premium", "/lookup", ""),
            Duration.ofMillis(-1));

    assertFalse(
        Validation.buildDefaultValidatorFactory().getValidator().validate(invalid).isEmpty());
  }

  @Test
  void validProviderConfigurationPassesValidation() {
    var valid =
        new ProviderProperties(
            new ProviderProperties.Endpoint("http://free", "/lookup", ""),
            new ProviderProperties.Endpoint("http://premium", "/lookup", ""),
            Duration.ofMillis(500));

    org.junit.jupiter.api.Assertions.assertTrue(
        Validation.buildDefaultValidatorFactory().getValidator().validate(valid).isEmpty());
  }
}
