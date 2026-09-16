package com.incode.verification.adapter.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.incode.verification.adapter.out.coordination.RedisCoordinationAdapter;
import com.incode.verification.application.port.out.CoordinationPort;
import com.incode.verification.application.port.out.VerificationView;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CoordinationProperties.class)
public class CoordinationConfiguration {
  @Bean
  Cache<String, VerificationView> verificationL1Cache(CoordinationProperties p) {
    return Caffeine.newBuilder().maximumSize(p.l1MaximumSize()).build();
  }

  @Bean
  CoordinationPort coordinationPort(
      Cache<String, VerificationView> cache,
      org.springframework.data.redis.core.StringRedisTemplate redis,
      CoordinationProperties p) {
    return new RedisCoordinationAdapter(cache, redis, p);
  }
}
