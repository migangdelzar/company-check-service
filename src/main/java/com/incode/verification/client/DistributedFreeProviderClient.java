package com.incode.verification.client;

import com.incode.verification.config.provider.ProviderEndpointProperties;
import com.incode.verification.exception.ProviderRateLimitExceededException;
import com.incode.verification.repository.ratelimit.ProviderRateLimiter;
import com.incode.verification.service.model.NormalizedQuery;
import com.incode.verification.service.model.ProviderResult;
import com.incode.verification.service.model.ProviderType;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.web.client.RestClient;

public class DistributedFreeProviderClient extends ProviderClientSupport {
  private final ProviderRateLimiter rateLimiter;

  public DistributedFreeProviderClient(ProviderClient delegate, ProviderRateLimiter rateLimiter) {
    super(delegate);
    this.rateLimiter = rateLimiter;
  }

  public DistributedFreeProviderClient(
      RestClient client, ProviderEndpointProperties endpoint, ProviderRateLimiter rateLimiter) {
    this(new FreeProviderClient(client, endpoint), rateLimiter);
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
