package com.incode.verification.config.coordination;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incode.verification.repository.CoordinationRepository;
import com.incode.verification.repository.coordination.RedisCoordinationRepository;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

@Configuration(proxyBeanMethods = false)
@Profile("distributed")
public class CoordinationConfiguration {
  @Bean
  CoordinationRepository coordination(
      ReactiveStringRedisTemplate redis,
      CacheManager cacheManager,
      CoordinationProperties properties,
      ObjectMapper mapper) {
    return new RedisCoordinationRepository(redis, cacheManager, properties, mapper);
  }
}
