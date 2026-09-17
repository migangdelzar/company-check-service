package com.incode.verification.repository.coordination;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incode.verification.config.coordination.CoordinationProperties;
import com.incode.verification.service.model.NormalizedQuery;
import com.incode.verification.service.model.VerificationResult;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class RedisCoordinationRepositoryTest {
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
    var repository = new RedisCoordinationRepository(null, null, properties, new ObjectMapper());
    var query = new NormalizedQuery("ACME");
    var result =
        new VerificationResult(null, "acme", "ACME", null, null, null, null, null, null, null);
    assertEquals(result, repository.put(query, result));
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
        new RedisCoordinationRepository(null, null, properties, new ObjectMapper())
            .acquire(new NormalizedQuery("ACME"));

    assertEquals(false, lease.acquired());
    assertEquals(true, lease.degraded());
  }
}
