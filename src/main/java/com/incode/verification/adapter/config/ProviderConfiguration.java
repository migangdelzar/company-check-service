package com.incode.verification.adapter.config;

import com.incode.verification.adapter.out.provider.ProviderContractException;
import com.incode.verification.adapter.out.provider.ProviderProperties;
import com.incode.verification.adapter.out.provider.ProviderTransientException;
import com.incode.verification.adapter.out.provider.RestClientProviderAdapter;
import com.incode.verification.application.context.ExecutionContext;
import com.incode.verification.application.port.out.ProviderLookupPort;
import com.incode.verification.domain.type.ProviderLookupResult;
import com.incode.verification.domain.type.ProviderType;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ProviderProperties.class)
public class ProviderConfiguration {
  @Bean("freeProviderClient")
  RestClient freeProviderClient(ProviderProperties p, CloseableHttpClient http) {
    return client(p.free(), http);
  }

  @Bean("premiumProviderClient")
  RestClient premiumProviderClient(ProviderProperties p, CloseableHttpClient http) {
    return client(p.premium(), http);
  }

  @Bean
  ProviderLookupPort freeProvider(
      @Qualifier("freeProviderClient") RestClient c, ProviderProperties p) {
    return new FreeResilientProviderAdapter(
        new RestClientProviderAdapter(c, ProviderType.FREE, p.free(), p.attemptTimeout()));
  }

  @Bean
  ProviderLookupPort premiumProvider(
      @Qualifier("premiumProviderClient") RestClient c, ProviderProperties p) {
    return new PremiumResilientProviderAdapter(
        new RestClientProviderAdapter(c, ProviderType.PREMIUM, p.premium(), p.attemptTimeout()));
  }

  @Bean
  CloseableHttpClient providerHttpClient(ProviderProperties p) {
    var timeout = p.attemptTimeout();
    var manager =
        PoolingHttpClientConnectionManagerBuilder.create()
            .setMaxConnTotal(100)
            .setMaxConnPerRoute(100)
            .build();
    Timeout requestTimeout = Timeout.ofMilliseconds(timeout.toMillis());
    RequestConfig rc =
        RequestConfig.custom()
            .setConnectionRequestTimeout(requestTimeout)
            .setResponseTimeout(requestTimeout)
            .build();
    return HttpClients.custom().setConnectionManager(manager).setDefaultRequestConfig(rc).build();
  }

  private RestClient client(ProviderProperties.Endpoint e, CloseableHttpClient http) {
    return RestClient.builder()
        .baseUrl(e.baseUrl())
        .requestFactory(new HttpComponentsClientHttpRequestFactory(http))
        .build();
  }

  static final class FreeResilientProviderAdapter implements ProviderLookupPort {
    private final ProviderLookupPort delegate;

    FreeResilientProviderAdapter(ProviderLookupPort d) {
      delegate = d;
    }

    @Override
    @Retry(name = "freeProvider", fallbackMethod = "fallback")
    @CircuitBreaker(name = "freeProvider", fallbackMethod = "fallback")
    @RateLimiter(name = "freeProvider")
    @Bulkhead(name = "freeProvider", type = Bulkhead.Type.SEMAPHORE)
    public ProviderLookupResult lookup(
        com.incode.verification.domain.valueobject.NormalizedQuery q, ExecutionContext c) {
      return delegate.lookup(q, c);
    }

    public ProviderLookupResult fallback(
        com.incode.verification.domain.valueobject.NormalizedQuery q,
        ExecutionContext c,
        Throwable failure) {
      return failureResult(failure);
    }
  }

  static final class PremiumResilientProviderAdapter implements ProviderLookupPort {
    private final ProviderLookupPort delegate;

    PremiumResilientProviderAdapter(ProviderLookupPort d) {
      delegate = d;
    }

    @Override
    @Retry(name = "premiumProvider", fallbackMethod = "fallback")
    @CircuitBreaker(name = "premiumProvider", fallbackMethod = "fallback")
    @RateLimiter(name = "premiumProvider")
    @Bulkhead(name = "premiumProvider", type = Bulkhead.Type.SEMAPHORE)
    public ProviderLookupResult lookup(
        com.incode.verification.domain.valueobject.NormalizedQuery q, ExecutionContext c) {
      return delegate.lookup(q, c);
    }

    public ProviderLookupResult fallback(
        com.incode.verification.domain.valueobject.NormalizedQuery q,
        ExecutionContext c,
        Throwable failure) {
      return failureResult(failure);
    }
  }

  private static ProviderLookupResult failureResult(Throwable failure) {
    if (failure instanceof ProviderTransientException transientFailure)
      return new ProviderLookupResult.Failure(transientFailure.failure());
    if (failure instanceof ProviderContractException)
      return new ProviderLookupResult.Failure(
          new com.incode.verification.domain.type.ProviderFailure.Malformed());
    return new ProviderLookupResult.Failure(
        new com.incode.verification.domain.type.ProviderFailure.Unavailable());
  }
}
