package com.incode.verification.adapter.out.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.incode.verification.application.port.out.InboundRateLimiter;
import com.incode.verification.configuration.InboundRateLimitProperties;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

class RedisInboundRateLimiterTest {
  private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
  private final RedisInboundRateLimiter limiter =
      new RedisInboundRateLimiter(
          redis, new InboundRateLimitProperties(2, Duration.ofSeconds(1), "test:inbound:"));

  @Test
  void allowsWhenRedisScriptReturnsOne() {
    when(redis.execute(
            any(DefaultRedisScript.class),
            eq(List.of("test:inbound:backend-service")),
            eq("1000"),
            eq("2")))
        .thenReturn(1L);

    assertEquals(InboundRateLimiter.Decision.Status.ALLOWED, limiter.tryAcquire().status());
  }

  @Test
  void rejectsWhenRedisScriptReturnsZero() {
    when(redis.execute(
            any(DefaultRedisScript.class),
            eq(List.of("test:inbound:backend-service")),
            eq("1000"),
            eq("2")))
        .thenReturn(0L);

    var decision = limiter.tryAcquire();

    assertEquals(InboundRateLimiter.Decision.Status.REJECTED, decision.status());
    assertEquals(Duration.ofSeconds(1), decision.retryAfter());
  }

  @Test
  void reportsUnavailableWhenRedisFails() {
    when(redis.execute(
            any(DefaultRedisScript.class),
            eq(List.of("test:inbound:backend-service")),
            eq("1000"),
            eq("2")))
        .thenThrow(new RuntimeException("Redis unavailable"));

    var decision = limiter.tryAcquire();

    assertEquals(InboundRateLimiter.Decision.Status.UNAVAILABLE, decision.status());
    assertEquals(Duration.ofSeconds(1), decision.retryAfter());
  }
}
