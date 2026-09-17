package com.incode.verification.configuration;

import com.incode.verification.adapter.out.provider.DistributedFreeProvider;
import com.incode.verification.adapter.out.provider.DistributedPremiumProvider;
import com.incode.verification.adapter.out.provider.ProviderProperties;
import com.incode.verification.adapter.out.provider.RestClientProviderAdapter;
import com.incode.verification.adapter.out.ratelimit.ProviderRateLimiter;
import com.incode.verification.adapter.out.ratelimit.RedisProviderRateLimiter;
import com.incode.verification.application.port.out.ProviderLookupPort;
import com.incode.verification.domain.provider.ProviderType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@Profile("distributed")
public class DistributedProviderResilienceConfiguration {
  @Bean
  ProviderRateLimiter providerRateLimiter(
      StringRedisTemplate redis, ProviderRateLimitProperties properties) {
    return new RedisProviderRateLimiter(redis, properties);
  }

  @Bean
  ProviderLookupPort freeProvider(
      @Qualifier("freeProviderClient") RestClient client,
      ProviderProperties properties,
      ProviderRateLimiter rateLimiter) {
    return new DistributedFreeProvider(
        new RestClientProviderAdapter(client, ProviderType.FREE, properties.free()), rateLimiter);
  }

  @Bean
  ProviderLookupPort premiumProvider(
      @Qualifier("premiumProviderClient") RestClient client,
      ProviderProperties properties,
      ProviderRateLimiter rateLimiter) {
    return new DistributedPremiumProvider(
        new RestClientProviderAdapter(client, ProviderType.PREMIUM, properties.premium()),
        rateLimiter);
  }
}
