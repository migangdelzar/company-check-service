package com.incode.verification.adapter.out.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.incode.verification.application.port.out.InboundRateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class Resilience4jInboundRateLimiterTest {
  @Test
  void allowsTheFirstRequest() {
    var limiter = new Resilience4jInboundRateLimiter(rateLimiter(1), Duration.ofSeconds(1));

    var decision = limiter.tryAcquire();

    assertEquals(InboundRateLimiter.Decision.Status.ALLOWED, decision.status());
  }

  @Test
  void rejectsImmediatelyWhenTheWindowIsExhausted() {
    var limiter = new Resilience4jInboundRateLimiter(rateLimiter(1), Duration.ofSeconds(2));

    limiter.tryAcquire();
    var decision = limiter.tryAcquire();

    assertEquals(InboundRateLimiter.Decision.Status.REJECTED, decision.status());
    assertEquals(Duration.ofSeconds(2), decision.retryAfter());
  }

  @Test
  void doesNotWaitForAFreePermit() {
    var limiter = new Resilience4jInboundRateLimiter(rateLimiter(1), Duration.ofSeconds(1));

    limiter.tryAcquire();
    var started = System.nanoTime();
    limiter.tryAcquire();
    var elapsed = Duration.ofNanos(System.nanoTime() - started);

    assertTrue(elapsed.compareTo(Duration.ofMillis(100)) < 0);
  }

  private static RateLimiter rateLimiter(int limit) {
    return RateLimiter.of(
        "backendService",
        RateLimiterConfig.custom()
            .limitForPeriod(limit)
            .limitRefreshPeriod(Duration.ofHours(1))
            .timeoutDuration(Duration.ZERO)
            .build());
  }
}
