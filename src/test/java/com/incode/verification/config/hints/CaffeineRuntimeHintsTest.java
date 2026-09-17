package com.incode.verification.config.hints;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;

class CaffeineRuntimeHintsTest {
  @Test
  void registersCaffeineRuntimeGeneratedMembers() {
    var hints = new RuntimeHints();
    new CaffeineRuntimeHints().registerHints(hints, getClass().getClassLoader());

    assertFields(
        hints, "com.github.benmanes.caffeine.cache.SSSMSA", "FACTORY", "expiresAfterAccessNanos");
    assertFields(hints, "com.github.benmanes.caffeine.cache.SSSMS", "maximum", "weightedSize");
    assertFields(hints, "com.github.benmanes.caffeine.cache.BoundedLocalCache", "refreshes");
    assertFields(hints, "com.github.benmanes.caffeine.cache.PS", "key", "value");
    assertFields(hints, "com.github.benmanes.caffeine.cache.PSW", "writeTime");
    assertFields(hints, "com.github.benmanes.caffeine.cache.StripedBuffer", "tableBusy");
    assertConstructorHint(hints, "com.github.benmanes.caffeine.cache.PSWMS");
  }

  private static void assertFields(RuntimeHints hints, String typeName, String... fieldNames) {
    var hint = hints.reflection().getTypeHint(TypeReference.of(typeName));
    assertNotNull(hint);
    for (var fieldName : fieldNames) {
      assertTrue(hint.fields().anyMatch(field -> field.getName().equals(fieldName)));
    }
  }

  private static void assertConstructorHint(RuntimeHints hints, String typeName) {
    var hint = hints.reflection().getTypeHint(TypeReference.of(typeName));
    assertNotNull(hint);
    assertTrue(hint.getMemberCategories().contains(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS));
  }
}
