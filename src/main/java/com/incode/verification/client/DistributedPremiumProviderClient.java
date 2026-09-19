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
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class DistributedPremiumProviderClient extends ProviderClientSupport {
  private final ProviderRateLimiter rateLimiter;

  public DistributedPremiumProviderClient(
      ProviderClient delegate, ProviderRateLimiter rateLimiter) {
    super(delegate);
    this.rateLimiter = rateLimiter;
  }

  public DistributedPremiumProviderClient(
      WebClient client, ProviderEndpointProperties endpoint, ProviderRateLimiter rateLimiter) {
    this(new PremiumProviderClient(client, endpoint), rateLimiter);
  }

  @Override
  @Retry(name = "premiumProvider", fallbackMethod = "fallback")
  @CircuitBreaker(name = "premiumProvider", fallbackMethod = "fallback")
  @Bulkhead(name = "premiumProvider", type = Bulkhead.Type.SEMAPHORE, fallbackMethod = "fallback")
  public Mono<ProviderResult> lookup(NormalizedQuery query) {
    return rateLimiter
        .tryAcquire(ProviderType.PREMIUM)
        .flatMap(
            allowed ->
                allowed
                    ? delegate.lookup(query)
                    : fallback(query, new ProviderRateLimitExceededException()));
  }
}
