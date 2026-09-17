package com.incode.verification.adapter.out.provider;

import com.incode.verification.adapter.out.ratelimit.ProviderRateLimiter;
import com.incode.verification.application.port.out.ProviderLookupPort;
import com.incode.verification.domain.provider.ProviderResult;
import com.incode.verification.domain.provider.ProviderType;
import com.incode.verification.domain.query.NormalizedQuery;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

public class DistributedFreeProvider extends ResilientProvider {
  private final ProviderRateLimiter rateLimiter;

  public DistributedFreeProvider(ProviderLookupPort delegate, ProviderRateLimiter rateLimiter) {
    super(delegate);
    this.rateLimiter = rateLimiter;
  }

  @Override
  @Retry(name = "freeProvider", fallbackMethod = "fallback")
  @CircuitBreaker(name = "freeProvider", fallbackMethod = "fallback")
  @Bulkhead(name = "freeProvider", type = Bulkhead.Type.SEMAPHORE, fallbackMethod = "fallback")
  public ProviderResult lookup(NormalizedQuery query) {
    if (!rateLimiter.tryAcquire(ProviderType.FREE)) {
      return fallback(query, new ProviderRateLimitExceededException());
    }
    return delegate.lookup(query);
  }
}
