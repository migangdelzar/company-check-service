package com.incode.verification.config;

import org.hibernate.validator.internal.util.logging.Log_$logger;
import org.hibernate.validator.internal.util.logging.Messages_$bundle;
import org.jspecify.annotations.Nullable;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * Keeps Hibernate Validator's generated JBoss Logging implementations reachable in native images.
 */
public final class HibernateValidatorRuntimeHints implements RuntimeHintsRegistrar {
  @Override
  public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
    hints.reflection().registerType(Log_$logger.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
    hints
        .reflection()
        .registerType(
            Messages_$bundle.class,
            MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
            MemberCategory.ACCESS_PUBLIC_FIELDS);
  }
}
