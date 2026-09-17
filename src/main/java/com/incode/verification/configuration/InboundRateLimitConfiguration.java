package com.incode.verification.configuration;

import com.incode.verification.adapter.out.ratelimit.RedisInboundRateLimiter;
import com.incode.verification.adapter.out.ratelimit.Resilience4jInboundRateLimiter;
import com.incode.verification.application.port.out.InboundRateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(InboundRateLimitProperties.class)
public class InboundRateLimitConfiguration {
  @Bean
  @Profile("single-node")
  InboundRateLimiter singleNodeInboundRateLimiter(
      RateLimiterRegistry registry, InboundRateLimitProperties properties) {
    return new Resilience4jInboundRateLimiter(
        registry.rateLimiter("backendService"), properties.refreshPeriod());
  }

  @Bean
  @Profile("distributed")
  InboundRateLimiter distributedInboundRateLimiter(
      StringRedisTemplate redis, InboundRateLimitProperties properties) {
    return new RedisInboundRateLimiter(redis, properties);
  }
}
