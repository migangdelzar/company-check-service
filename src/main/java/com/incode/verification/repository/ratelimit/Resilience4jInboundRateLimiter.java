package com.incode.verification.repository.ratelimit;

import com.incode.verification.repository.InboundRateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiter;
import java.time.Duration;

public final class Resilience4jInboundRateLimiter implements InboundRateLimiter {
  private final RateLimiter limiter;
  private final Duration retryAfter;

  public Resilience4jInboundRateLimiter(RateLimiter limiter, Duration retryAfter) {
    this.limiter = limiter;
    this.retryAfter = retryAfter;
  }

  @Override
  public Decision tryAcquire() {
    return limiter.acquirePermission()
        ? new Decision(Decision.Status.ALLOWED, Duration.ZERO)
        : new Decision(Decision.Status.REJECTED, retryAfter);
  }
}
