package com.incode.verification.repository.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.incode.verification.repository.InboundRateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class Resilience4jInboundRateLimiterTest {
  @Test
  void allowsTheFirstRequestAndRejectsTheNextWithoutWaiting() {
    var limiter = new Resilience4jInboundRateLimiter(rateLimiter(1), Duration.ofSeconds(2));

    assertEquals(InboundRateLimiter.Decision.Status.ALLOWED, limiter.tryAcquire().block().status());
    var decision = limiter.tryAcquire().block();
    assertEquals(InboundRateLimiter.Decision.Status.REJECTED, decision.status());
    assertEquals(Duration.ofSeconds(2), decision.retryAfter());
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
