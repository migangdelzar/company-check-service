package com.incode.verification.repository.ratelimit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.incode.verification.config.provider.ProviderRateLimitProperties;
import com.incode.verification.service.model.ProviderType;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import reactor.core.publisher.Flux;

class RedisProviderRateLimiterTest {
  private final ReactiveStringRedisTemplate redis = mock(ReactiveStringRedisTemplate.class);
  private final RedisProviderRateLimiter limiter =
      new RedisProviderRateLimiter(redis, properties());

  @Test
  void allowsWhenRedisScriptReturnsOne() {
    when(redis.execute(any(DefaultRedisScript.class), anyList(), any(Object[].class)))
        .thenReturn(Flux.just(1L));
    assertTrue(limiter.tryAcquire(ProviderType.FREE).block());
  }

  @Test
  void rejectsWhenRedisScriptReturnsZeroOrFails() {
    when(redis.execute(any(DefaultRedisScript.class), anyList(), any(Object[].class)))
        .thenReturn(Flux.just(0L));
    assertFalse(limiter.tryAcquire(ProviderType.FREE).block());
  }

  private static ProviderRateLimitProperties properties() {
    return new ProviderRateLimitProperties(
        new ProviderRateLimitProperties.Limit(2, Duration.ofSeconds(1)),
        new ProviderRateLimitProperties.Limit(3, Duration.ofSeconds(1)),
        "test:rate:");
  }
}
