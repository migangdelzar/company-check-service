package com.incode.verification.config;

import com.incode.verification.client.dto.FreeCompanyResponse;
import com.incode.verification.client.dto.PremiumCompanyResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

public final class ProviderRuntimeHints implements RuntimeHintsRegistrar {
  @Override
  public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
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
