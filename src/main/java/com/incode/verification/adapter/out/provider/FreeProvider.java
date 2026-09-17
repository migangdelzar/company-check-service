package com.incode.verification.adapter.out.provider;

import com.incode.verification.application.port.out.ProviderLookupPort;
import com.incode.verification.domain.provider.ProviderResult;
import com.incode.verification.domain.query.NormalizedQuery;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;

public class FreeProvider extends ResilientProvider {
  public FreeProvider(ProviderLookupPort delegate) {
    super(delegate);
  }

  @Override
  @Retry(name = "freeProvider", fallbackMethod = "fallback")
  @CircuitBreaker(name = "freeProvider", fallbackMethod = "fallback")
  @RateLimiter(name = "freeProvider", fallbackMethod = "fallback")
  @Bulkhead(name = "freeProvider", type = Bulkhead.Type.SEMAPHORE, fallbackMethod = "fallback")
  public ProviderResult lookup(NormalizedQuery query) {
    return delegate.lookup(query);
  }
}
