package com.incode.verification.repository.coordination;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incode.verification.config.coordination.CoordinationProperties;
import com.incode.verification.service.model.NormalizedQuery;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;

class RedisCoordinationRepositoryTest {
  @Test
  void marksRedisFailureAsDegradedWithoutGrantingLookupOwnership() {
    var redis = mock(ReactiveStringRedisTemplate.class);
    var values = mock(ReactiveValueOperations.class);
    when(redis.opsForValue()).thenReturn(values);
    when(values.setIfAbsent(anyString(), anyString(), any(Duration.class)))
        .thenReturn(Mono.error(new RuntimeException("redis unavailable")));
    var lease =
        new RedisCoordinationRepository(
                redis,
                new ConcurrentMapCacheManager("verification"),
                properties(),
                new ObjectMapper())
            .acquire(new NormalizedQuery("ACME"))
            .block();

    assertFalse(lease.acquired());
    assertTrue(lease.degraded());
  }

  private static CoordinationProperties properties() {
    return new CoordinationProperties(
        1,
        Duration.ofMinutes(1),
        Duration.ofMinutes(1),
        Duration.ofMinutes(1),
        Duration.ZERO,
        Duration.ofSeconds(1),
        Duration.ZERO,
        0,
        "test:");
  }
}
