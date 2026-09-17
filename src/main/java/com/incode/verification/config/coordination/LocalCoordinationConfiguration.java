package com.incode.verification.config.coordination;

import com.incode.verification.repository.CoordinationRepository;
import com.incode.verification.repository.coordination.LocalCoordinationRepository;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("single-node")
public class LocalCoordinationConfiguration {
  @Bean
  CoordinationRepository coordination(CacheManager cacheManager) {
    return new LocalCoordinationRepository(cacheManager);
  }
}
