package com.incode.verification.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incode.verification.adapter.out.coordination.RedisCoordinationAdapter;
import com.incode.verification.application.port.out.CoordinationPort;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration(proxyBeanMethods = false)
@Profile("distributed")
public class CoordinationConfiguration {
  @Bean
  CoordinationPort coordination(
      StringRedisTemplate redis,
      CacheManager cacheManager,
      CoordinationProperties properties,
      ObjectMapper mapper) {
    return new RedisCoordinationAdapter(redis, cacheManager, properties, mapper);
  }
}
