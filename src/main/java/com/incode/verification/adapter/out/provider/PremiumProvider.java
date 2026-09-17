package com.incode.verification.adapter.out.provider;

import com.incode.verification.application.port.out.ProviderLookupPort;
import com.incode.verification.domain.provider.ProviderResult;
import com.incode.verification.domain.query.NormalizedQuery;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;

public class PremiumProvider extends ResilientProvider {
  public PremiumProvider(ProviderLookupPort delegate) {
    super(delegate);
  }

  @Override
  @Retry(name = "premiumProvider", fallbackMethod = "fallback")
  @CircuitBreaker(name = "premiumProvider", fallbackMethod = "fallback")
  @RateLimiter(name = "premiumProvider", fallbackMethod = "fallback")
  @Bulkhead(name = "premiumProvider", type = Bulkhead.Type.SEMAPHORE, fallbackMethod = "fallback")
  public ProviderResult lookup(NormalizedQuery query) {
    return delegate.lookup(query);
  }
}
