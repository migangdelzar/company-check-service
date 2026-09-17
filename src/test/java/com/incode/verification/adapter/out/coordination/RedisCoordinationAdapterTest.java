package com.incode.verification.adapter.out.coordination;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incode.verification.configuration.CoordinationProperties;
import com.incode.verification.application.result.VerificationResult;
import com.incode.verification.domain.query.NormalizedQuery;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class RedisCoordinationAdapterTest {
  @Test
  void putReturnsImmutableCachedResultWithoutRedisRoundTrip() {
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
    var adapter = new RedisCoordinationAdapter(null, null, properties, new ObjectMapper());
    var query = new NormalizedQuery("ACME");
    var result = new VerificationResult(null, "acme", "ACME", null, null, null, null, null, null, null);
    assertEquals(result, adapter.put(query, result));
  }

  @Test
  void marksRedisFailureAsDegradedWithoutGrantingLookupOwnership() {
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

    var lease =
            new RedisCoordinationAdapter(null, null, properties, new ObjectMapper())
            .acquire(
                new NormalizedQuery("ACME"));

    assertEquals(false, lease.acquired());
    assertEquals(true, lease.degraded());
  }
}
