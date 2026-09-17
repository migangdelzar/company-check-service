package com.incode.verification.configuration;

import com.incode.verification.adapter.out.coordination.LocalCoordinationAdapter;
import com.incode.verification.application.port.out.CoordinationPort;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("single-node")
public class LocalCoordinationConfiguration {
  @Bean
  CoordinationPort coordination(CacheManager cacheManager) {
    return new LocalCoordinationAdapter(cacheManager);
  }
}
