package com.incode.verification.client;

import com.incode.verification.config.ProviderEndpointProperties;
import com.incode.verification.exception.ProviderRateLimitExceededException;
import com.incode.verification.repository.ratelimit.ProviderRateLimiter;
import com.incode.verification.service.model.NormalizedQuery;
import com.incode.verification.service.model.ProviderResult;
import com.incode.verification.service.model.ProviderType;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.web.client.RestClient;

public class DistributedPremiumProviderClient extends ProviderClientSupport {
  private final ProviderRateLimiter rateLimiter;

  public DistributedPremiumProviderClient(
      ProviderClient delegate, ProviderRateLimiter rateLimiter) {
    super(delegate);
    this.rateLimiter = rateLimiter;
  }

  public DistributedPremiumProviderClient(
      RestClient client, ProviderEndpointProperties endpoint, ProviderRateLimiter rateLimiter) {
    this(new PremiumProviderClient(client, endpoint), rateLimiter);
  }

  @Override
  @Retry(name = "premiumProvider", fallbackMethod = "fallback")
  @CircuitBreaker(name = "premiumProvider", fallbackMethod = "fallback")
  @Bulkhead(name = "premiumProvider", type = Bulkhead.Type.SEMAPHORE, fallbackMethod = "fallback")
  public ProviderResult lookup(NormalizedQuery query) {
    if (!rateLimiter.tryAcquire(ProviderType.PREMIUM)) {
      return fallback(query, new ProviderRateLimitExceededException());
    }
    return delegate.lookup(query);
  }
}
