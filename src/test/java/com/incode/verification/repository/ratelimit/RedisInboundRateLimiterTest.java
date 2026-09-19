package com.incode.verification.repository.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.incode.verification.config.ratelimit.InboundRateLimitProperties;
import com.incode.verification.repository.InboundRateLimiter;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import reactor.core.publisher.Flux;

class RedisInboundRateLimiterTest {
  private final ReactiveStringRedisTemplate redis = mock(ReactiveStringRedisTemplate.class);
  private final RedisInboundRateLimiter limiter =
      new RedisInboundRateLimiter(
          redis, new InboundRateLimitProperties(2, Duration.ofSeconds(1), "test:inbound:"));

  @Test
  void mapsRedisDecisionsToReactiveRateLimitDecisions() {
    when(redis.execute(any(DefaultRedisScript.class), anyList(), any(Object[].class)))
        .thenReturn(Flux.just(1L), Flux.just(0L));

    assertEquals(InboundRateLimiter.Decision.Status.ALLOWED, limiter.tryAcquire().block().status());
    var rejected = limiter.tryAcquire().block();
    assertEquals(InboundRateLimiter.Decision.Status.REJECTED, rejected.status());
    assertEquals(Duration.ofSeconds(1), rejected.retryAfter());
  }
}
