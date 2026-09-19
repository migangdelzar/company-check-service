package com.incode.verification.client;

import com.incode.verification.client.dto.FreeCompanyResponse;
import com.incode.verification.config.provider.ProviderEndpointProperties;
import com.incode.verification.mapper.ProviderMapper;
import com.incode.verification.service.model.NormalizedQuery;
import com.incode.verification.service.model.ProviderResult;
import com.incode.verification.service.model.ProviderType;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class FreeProviderClient extends ProviderClientSupport {
  public FreeProviderClient(ProviderClient delegate) {
    super(delegate);
  }

  public FreeProviderClient(WebClient client, ProviderEndpointProperties endpoint) {
    this(
        new TypedProviderClient<>(
            client,
            endpoint,
            ProviderType.FREE,
            FreeCompanyResponse[].class,
            ProviderMapper::mapFree));
  }

  @Override
  @Retry(name = "freeProvider", fallbackMethod = "fallback")
  @CircuitBreaker(name = "freeProvider", fallbackMethod = "fallback")
  @RateLimiter(name = "freeProvider", fallbackMethod = "fallback")
  @Bulkhead(name = "freeProvider", type = Bulkhead.Type.SEMAPHORE, fallbackMethod = "fallback")
  public Mono<ProviderResult> lookup(NormalizedQuery query) {
    return delegate.lookup(query);
  }
}
