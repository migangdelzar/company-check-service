package com.incode.verification.config.provider;

import com.incode.verification.client.FreeProviderClient;
import com.incode.verification.client.PremiumProviderClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration(proxyBeanMethods = false)
@Profile("single-node")
public class ProviderResilienceConfiguration {
  @Bean
  FreeProviderClient freeProvider(
      @Qualifier("freeProviderClient") WebClient client, ProviderProperties properties) {
    return new FreeProviderClient(client, properties.free());
  }

  @Bean
  PremiumProviderClient premiumProvider(
      @Qualifier("premiumProviderClient") WebClient client, ProviderProperties properties) {
    return new PremiumProviderClient(client, properties.premium());
  }
}
