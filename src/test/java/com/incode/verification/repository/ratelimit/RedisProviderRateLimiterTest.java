package com.incode.verification.repository.ratelimit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.incode.verification.config.provider.ProviderRateLimitProperties;
import com.incode.verification.service.model.ProviderType;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

class RedisProviderRateLimiterTest {
  private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
  private final RedisProviderRateLimiter limiter =
      new RedisProviderRateLimiter(redis, properties());

  @Test
  void allowsWhenRedisScriptReturnsOne() {
    when(redis.execute(
            any(DefaultRedisScript.class), eq(List.of("test:rate:free")), eq("1000"), eq("2")))
        .thenReturn(1L);

    assertTrue(limiter.tryAcquire(ProviderType.FREE));
  }

  @Test
  void rejectsWhenRedisScriptReturnsZero() {
    when(redis.execute(
            any(DefaultRedisScript.class), eq(List.of("test:rate:free")), eq("1000"), eq("2")))
        .thenReturn(0L);

    assertFalse(limiter.tryAcquire(ProviderType.FREE));
  }

  @Test
  void rejectsWhenRedisIsUnavailable() {
    when(redis.execute(
            any(DefaultRedisScript.class), eq(List.of("test:rate:free")), eq("1000"), eq("2")))
        .thenThrow(new RuntimeException("Redis unavailable"));

    assertFalse(limiter.tryAcquire(ProviderType.FREE));
  }

  private static ProviderRateLimitProperties properties() {
    return new ProviderRateLimitProperties(
        new ProviderRateLimitProperties.Limit(2, Duration.ofSeconds(1)),
        new ProviderRateLimitProperties.Limit(3, Duration.ofSeconds(1)),
        "test:rate:");
  }
}
