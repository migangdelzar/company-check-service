package com.incode.verification.adapter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.incode.verification.adapter.out.coordination.RedisCoordinationAdapter;
import com.incode.verification.application.port.out.CoordinationPort;
import com.incode.verification.application.port.out.VerificationView;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration(proxyBeanMethods = false)
public class CoordinationConfiguration {
  @Bean("verificationL1Cache")
  Cache<String, VerificationView> verificationL1Cache(CoordinationProperties p) {
    return Caffeine.newBuilder()
        .maximumSize(p.l1MaximumSize())
        .expireAfter(
            new Expiry<String, VerificationView>() {
              @Override
              public long expireAfterCreate(String key, VerificationView value, long now) {
                return p.ttlFor(value).toNanos();
              }

              @Override
              public long expireAfterUpdate(
                  String key, VerificationView value, long now, long currentDuration) {
                return currentDuration;
              }

              @Override
              public long expireAfterRead(
                  String key, VerificationView value, long now, long currentDuration) {
                return currentDuration;
              }
            })
        .build();
  }

  @Bean
  CoordinationPort coordinationPort(
      @Qualifier("verificationL1Cache") Cache<String, VerificationView> cache,
      StringRedisTemplate redis,
      CoordinationProperties p,
      ObjectMapper mapper) {
    return new RedisCoordinationAdapter(cache, redis, p, mapper);
  }
}
