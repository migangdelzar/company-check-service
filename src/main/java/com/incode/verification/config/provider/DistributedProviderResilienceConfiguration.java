package com.incode.verification.config.provider;

import com.incode.verification.client.DistributedFreeProviderClient;
import com.incode.verification.client.DistributedPremiumProviderClient;
import com.incode.verification.repository.ratelimit.ProviderRateLimiter;
import com.incode.verification.repository.ratelimit.RedisProviderRateLimiter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration(proxyBeanMethods = false)
@Profile("distributed")
public class DistributedProviderResilienceConfiguration {
  @Bean
  ProviderRateLimiter providerRateLimiter(
      ReactiveStringRedisTemplate redis, ProviderRateLimitProperties properties) {
    return new RedisProviderRateLimiter(redis, properties);
  }

  @Bean
  DistributedFreeProviderClient freeProvider(
      @Qualifier("freeProviderClient") WebClient client,
      ProviderProperties properties,
      ProviderRateLimiter rateLimiter) {
    return new DistributedFreeProviderClient(client, properties.free(), rateLimiter);
  }

  @Bean
  DistributedPremiumProviderClient premiumProvider(
      @Qualifier("premiumProviderClient") WebClient client,
      ProviderProperties properties,
      ProviderRateLimiter rateLimiter) {
    return new DistributedPremiumProviderClient(client, properties.premium(), rateLimiter);
  }
}
