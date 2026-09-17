package com.incode.verification.config;

import org.hibernate.validator.internal.util.logging.Log_$logger;
import org.hibernate.validator.internal.util.logging.Messages_$bundle;
import org.jspecify.annotations.Nullable;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;

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
            type ->
                type.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)
                    .withField("INSTANCE"));
    hints
        .reflection()
        .registerType(
            TypeReference.of("org.hibernate.validator.internal.util.logging.Messages_$bundle_en"));
    hints
        .reflection()
        .registerType(
            TypeReference.of(
                "org.hibernate.validator.internal.util.logging.Messages_$bundle_en_US"));
    hints
        .reflection()
        .registerType(
            TypeReference.of(
                "org.hibernate.validator.internal.util.logging.Messages_$bundle_en_MX"));
  }
}
