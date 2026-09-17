package com.incode.verification.configuration.coordination;

import com.incode.verification.adapter.out.coordination.LocalExpirationLock;
import com.incode.verification.adapter.out.coordination.RedisExpirationLock;
import com.incode.verification.application.port.out.ExpirationLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration(proxyBeanMethods = false)
public class ExpirationLockConfiguration {
  @Bean
  @Profile("single-node")
  ExpirationLock localExpirationLock() {
    return new LocalExpirationLock();
  }

  @Bean
  @Profile("distributed")
  ExpirationLock redisExpirationLock(
      StringRedisTemplate redis, ExpirationLockProperties properties) {
    return new RedisExpirationLock(redis, properties);
  }
}
