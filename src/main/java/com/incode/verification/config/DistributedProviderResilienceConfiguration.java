package com.incode.verification.config;

import com.incode.verification.client.DistributedFreeProviderClient;
import com.incode.verification.client.DistributedPremiumProviderClient;
import com.incode.verification.client.ProviderClient;
import com.incode.verification.repository.ratelimit.ProviderRateLimiter;
import com.incode.verification.repository.ratelimit.RedisProviderRateLimiter;
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
  ProviderClient freeProvider(
      @Qualifier("freeProviderClient") RestClient client,
      ProviderProperties properties,
      ProviderRateLimiter rateLimiter) {
    return new DistributedFreeProviderClient(client, properties.free(), rateLimiter);
  }

  @Bean
  ProviderClient premiumProvider(
      @Qualifier("premiumProviderClient") RestClient client,
      ProviderProperties properties,
      ProviderRateLimiter rateLimiter) {
    return new DistributedPremiumProviderClient(client, properties.premium(), rateLimiter);
  }
}
