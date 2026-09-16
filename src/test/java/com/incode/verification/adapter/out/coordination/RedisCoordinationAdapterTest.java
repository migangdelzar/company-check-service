package com.incode.verification.adapter.out.coordination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.incode.verification.adapter.config.CoordinationProperties;
import com.incode.verification.application.port.out.VerificationView;
import com.incode.verification.domain.valueobject.LookupKey;
import com.incode.verification.domain.valueobject.NormalizedQuery;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class RedisCoordinationAdapterTest {
  @Test
  void l1ReturnsImmutableCachedViewWithoutRedisRoundTrip() {
    Cache<String, VerificationView> cache = Caffeine.newBuilder().maximumSize(1).build();
    var properties =
        new CoordinationProperties(
            1,
            Duration.ofMinutes(1),
            Duration.ofMinutes(1),
            Duration.ofMinutes(1),
            Duration.ZERO,
            Duration.ofSeconds(1),
            Duration.ZERO,
            0,
            "test:");
    var adapter = new RedisCoordinationAdapter(cache, null, properties);
    var key = new LookupKey(new NormalizedQuery("ACME"));
    var view = new VerificationView(null, "acme", "ACME", null, null, null, null, null, null, null);
    adapter.cache(key, view);
    assertTrue(adapter.cached(key).isPresent());
    assertEquals(view, adapter.cached(key).orElseThrow());
  }
}
