package com.incode.verification.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hibernate.validator.internal.util.logging.Log_$logger;
import org.hibernate.validator.internal.util.logging.Messages_$bundle;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;

class HibernateValidatorRuntimeHintsTest {
  @Test
  void registersGeneratedLoggerImplementationsForNativeImages() {
    var hints = new RuntimeHints();
    new HibernateValidatorRuntimeHints().registerHints(hints, getClass().getClassLoader());

    assertConstructorHint(hints, Log_$logger.class);
    assertConstructorHint(hints, Messages_$bundle.class);
    var bundleHint = hints.reflection().getTypeHint(Messages_$bundle.class);
    assertNotNull(bundleHint);
    assertTrue(bundleHint.fields().anyMatch(field -> field.getName().equals("INSTANCE")));
    assertTypeHint(hints, "org.hibernate.validator.internal.util.logging.Messages_$bundle_en");
    assertTypeHint(hints, "org.hibernate.validator.internal.util.logging.Messages_$bundle_en_US");
    assertTypeHint(hints, "org.hibernate.validator.internal.util.logging.Messages_$bundle_en_MX");
  }

  private static void assertTypeHint(RuntimeHints hints, String typeName) {
    assertNotNull(hints.reflection().getTypeHint(TypeReference.of(typeName)));
  }

  private static void assertConstructorHint(RuntimeHints hints, Class<?> type) {
    var hint = hints.reflection().getTypeHint(type);
    assertNotNull(hint);
    assertTrue(hint.getMemberCategories().contains(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS));
  }
}
