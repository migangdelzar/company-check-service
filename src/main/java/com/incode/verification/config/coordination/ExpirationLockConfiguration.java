package com.incode.verification.config.coordination;

import com.incode.verification.repository.ExpirationLock;
import com.incode.verification.repository.coordination.LocalExpirationLock;
import com.incode.verification.repository.coordination.RedisExpirationLock;
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
