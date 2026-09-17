package com.incode.verification.config.hints;

import com.incode.verification.client.dto.FreeCompanyResponse;
import com.incode.verification.client.dto.PremiumCompanyResponse;
import com.incode.verification.config.provider.ProviderEndpointProperties;
import com.incode.verification.config.provider.ProviderProperties;
import org.jspecify.annotations.Nullable;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

public final class ProviderRuntimeHints implements RuntimeHintsRegistrar {
  @Override
  public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
    hints
        .reflection()
        .registerType(
            ProviderEndpointProperties.class,
            MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
            MemberCategory.INVOKE_PUBLIC_METHODS,
            MemberCategory.ACCESS_DECLARED_FIELDS);
    hints
        .reflection()
        .registerType(
            ProviderProperties.HttpPoolProperties.class,
            MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
            MemberCategory.ACCESS_DECLARED_FIELDS);
    registerProviderResponse(hints, FreeCompanyResponse.class);
    registerProviderResponse(hints, PremiumCompanyResponse.class);
  }

  private static void registerProviderResponse(RuntimeHints hints, Class<?> responseType) {
    hints
        .reflection()
        .registerType(
            responseType,
            MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
            MemberCategory.INVOKE_PUBLIC_METHODS,
            MemberCategory.ACCESS_DECLARED_FIELDS);
  }
}
