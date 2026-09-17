package com.incode.verification.configuration.coordination;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableCaching
public class CacheConfiguration {
  @Bean
  CacheManager cacheManager(CoordinationProperties properties) {
    var manager = new CaffeineCacheManager("verification");
    manager.setCaffeine(
        Caffeine.newBuilder()
            .maximumSize(properties.l1MaximumSize())
            .expireAfter(new VerificationCacheExpiry(properties))
            .recordStats());
    return manager;
  }
}
