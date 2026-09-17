package com.incode.verification.config;

import org.hibernate.validator.internal.util.logging.Log_$logger;
import org.hibernate.validator.internal.util.logging.Messages_$bundle;
import org.jspecify.annotations.Nullable;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/** Keeps Hibernate Validator's generated JBoss Logging implementations reachable in native images. */
public final class HibernateValidatorRuntimeHints implements RuntimeHintsRegistrar {
  @Override
  public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
    registerLoggerType(hints, Log_$logger.class);
    registerLoggerType(hints, Messages_$bundle.class);
  }

  private static void registerLoggerType(RuntimeHints hints, Class<?> loggerType) {
    hints
        .reflection()
        .registerType(loggerType, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
  }
}
