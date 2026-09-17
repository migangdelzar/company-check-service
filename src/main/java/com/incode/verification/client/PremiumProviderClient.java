package com.incode.verification.client;

import com.incode.verification.client.dto.PremiumCompanyResponse;
import com.incode.verification.config.provider.ProviderEndpointProperties;
import com.incode.verification.mapper.ProviderMapper;
import com.incode.verification.service.model.NormalizedQuery;
import com.incode.verification.service.model.ProviderResult;
import com.incode.verification.service.model.ProviderType;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.web.client.RestClient;

public class PremiumProviderClient extends ProviderClientSupport {
  public PremiumProviderClient(ProviderClient delegate) {
    super(delegate);
  }

  public PremiumProviderClient(RestClient client, ProviderEndpointProperties endpoint) {
    this(
        new TypedProviderClient<>(
            client,
            endpoint,
            ProviderType.PREMIUM,
            PremiumCompanyResponse[].class,
            ProviderMapper::mapPremium));
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
