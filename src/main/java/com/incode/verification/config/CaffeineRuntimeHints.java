package com.incode.verification.config;

import org.jspecify.annotations.Nullable;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;

/** Keeps Caffeine's runtime-generated cache implementations reachable in native images. */
public final class CaffeineRuntimeHints implements RuntimeHintsRegistrar {
  private static final TypeReference CAFFEINE_CACHE_IMPLEMENTATION =
      TypeReference.of("com.github.benmanes.caffeine.cache.SSSMSA");
  private static final TypeReference[] TYPE_ONLY_HINTS = {
    TypeReference.of("com.github.benmanes.caffeine.cache.AsyncCache"),
    TypeReference.of("com.github.benmanes.caffeine.cache.AsyncCacheLoader"),
    TypeReference.of("com.github.benmanes.caffeine.cache.Cache"),
    TypeReference.of("com.github.benmanes.caffeine.cache.CacheLoader"),
    TypeReference.of("com.github.benmanes.caffeine.cache.Caffeine"),
    TypeReference.of("com.github.benmanes.caffeine.cache.CaffeineSpec")
  };

  @Override
  public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
    hints
        .reflection()
        .registerType(
            CAFFEINE_CACHE_IMPLEMENTATION,
            type ->
                type.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)
                    .withField("FACTORY")
                    .withField("expiresAfterAccessNanos"));
    for (var type : TYPE_ONLY_HINTS) {
      hints.reflection().registerType(type);
    }
    registerFields(
        hints,
        "com.github.benmanes.caffeine.cache.BLCHeader$DrainStatusRef",
        "drainStatus");
    registerFields(
        hints,
        "com.github.benmanes.caffeine.cache.BaseMpscLinkedArrayQueueColdProducerFields",
        "producerLimit");
    registerFields(
        hints,
        "com.github.benmanes.caffeine.cache.BaseMpscLinkedArrayQueueConsumerFields",
        "consumerIndex");
    registerFields(
        hints,
        "com.github.benmanes.caffeine.cache.BaseMpscLinkedArrayQueueProducerFields",
        "producerIndex");
    registerFields(hints, "com.github.benmanes.caffeine.cache.BoundedLocalCache", "refreshes");
    registerFields(hints, "com.github.benmanes.caffeine.cache.PS", "key", "value");
    registerFields(hints, "com.github.benmanes.caffeine.cache.PSW", "writeTime");
    hints
        .reflection()
        .registerType(
            TypeReference.of("com.github.benmanes.caffeine.cache.PSWMS"),
            type -> type.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS));
    registerFields(hints, "com.github.benmanes.caffeine.cache.SSSMS", "maximum", "weightedSize");
    registerFields(hints, "com.github.benmanes.caffeine.cache.StripedBuffer", "tableBusy");
    registerFields(hints, "com.github.benmanes.caffeine.cache.UnboundedLocalCache", "refreshes");
  }

  private static void registerFields(RuntimeHints hints, String typeName, String... fieldNames) {
    hints
        .reflection()
        .registerType(
            TypeReference.of(typeName),
            type -> {
              var builder = type;
              for (var fieldName : fieldNames) {
                builder = builder.withField(fieldName);
              }
            });
  }
}
