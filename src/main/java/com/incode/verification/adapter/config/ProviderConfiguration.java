package com.incode.verification.adapter.config;

import com.incode.verification.adapter.out.provider.*;
import com.incode.verification.application.context.ExecutionContext;
import com.incode.verification.application.port.out.ProviderLookupPort;
import com.incode.verification.domain.type.*;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import java.time.Duration;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(ProviderProperties.class)
public class ProviderConfiguration {
    @Bean("freeProviderClient") RestClient freeProviderClient(ProviderProperties p) { return client(p.free(), p.attemptTimeout()); }
    @Bean("premiumProviderClient") RestClient premiumProviderClient(ProviderProperties p) { return client(p.premium(), p.attemptTimeout()); }
    @Bean ProviderLookupPort freeProvider(@Qualifier("freeProviderClient") RestClient c, ProviderProperties p) { return resilient(new RestClientProviderAdapter(c, ProviderType.FREE, p.free(), p.attemptTimeout())); }
    @Bean ProviderLookupPort premiumProvider(@Qualifier("premiumProviderClient") RestClient c, ProviderProperties p) { return resilient(new RestClientProviderAdapter(c, ProviderType.PREMIUM, p.premium(), p.attemptTimeout())); }
    @Bean @Primary ProviderLookupPort providerResolver(@Qualifier("freeProvider") ProviderLookupPort free, @Qualifier("premiumProvider") ProviderLookupPort premium) { return new ProviderResolver(free, premium); }
    private RestClient client(ProviderProperties.Endpoint e, Duration timeout) {
        var manager = PoolingHttpClientConnectionManagerBuilder.create().setMaxConnTotal(100).setMaxConnPerRoute(100).build();
        RequestConfig rc = RequestConfig.custom().setConnectionRequestTimeout(timeout).setResponseTimeout(timeout).build();
        CloseableHttpClient http = HttpClients.custom().setConnectionManager(manager).setDefaultRequestConfig(rc).build();
        return RestClient.builder().baseUrl(e.baseUrl()).requestFactory(new HttpComponentsClientHttpRequestFactory(http)).build();
    }
    private ProviderLookupPort resilient(ProviderLookupPort delegate) { return new ResilientProviderAdapter(delegate); }
    static final class ResilientProviderAdapter implements ProviderLookupPort {
        private final ProviderLookupPort delegate; ResilientProviderAdapter(ProviderLookupPort d) { delegate = d; }
        @Retry(name = "provider") @CircuitBreaker(name = "provider") @RateLimiter(name = "provider") @Bulkhead(name = "provider", type = Bulkhead.Type.SEMAPHORE)
        public ProviderLookupResult lookup(com.incode.verification.domain.valueobject.NormalizedQuery q, ExecutionContext c) { return delegate.lookup(q, c); }
    }
}
