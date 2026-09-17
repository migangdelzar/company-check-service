package com.incode.verification.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.incode.verification.client.dto.FreeCompanyResponse;
import com.incode.verification.client.dto.PremiumCompanyResponse;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;

class ProviderRuntimeHintsTest {
  @Test
  void registersProviderResponsesForNativeJsonBinding() {
    var hints = new RuntimeHints();
    new ProviderRuntimeHints().registerHints(hints, getClass().getClassLoader());

    assertConstructorHint(hints, ProviderEndpointProperties.class);
    assertPublicMethodHint(hints, ProviderEndpointProperties.class);
    assertConstructorHint(hints, ProviderProperties.HttpPoolProperties.class);
    assertResponseHint(hints, FreeCompanyResponse.class);
    assertResponseHint(hints, PremiumCompanyResponse.class);
  }

  private static void assertConstructorHint(RuntimeHints hints, Class<?> type) {
    var hint = hints.reflection().getTypeHint(type);
    assertNotNull(hint);
    assertTrue(hint.getMemberCategories().contains(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS));
    assertTrue(hint.getMemberCategories().contains(MemberCategory.ACCESS_DECLARED_FIELDS));
  }

  private static void assertResponseHint(RuntimeHints hints, Class<?> responseType) {
    var hint = hints.reflection().getTypeHint(responseType);
    assertNotNull(hint);
    assertTrue(hint.getMemberCategories().contains(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS));
    assertTrue(hint.getMemberCategories().contains(MemberCategory.INVOKE_PUBLIC_METHODS));
    assertTrue(hint.getMemberCategories().contains(MemberCategory.ACCESS_DECLARED_FIELDS));
  }

  private static void assertPublicMethodHint(RuntimeHints hints, Class<?> type) {
    var hint = hints.reflection().getTypeHint(type);
    assertNotNull(hint);
    assertTrue(hint.getMemberCategories().contains(MemberCategory.INVOKE_PUBLIC_METHODS));
  }
}
