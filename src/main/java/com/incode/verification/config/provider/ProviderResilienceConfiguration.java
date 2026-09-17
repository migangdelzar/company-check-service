package com.incode.verification.config.provider;

import com.incode.verification.client.FreeProviderClient;
import com.incode.verification.client.PremiumProviderClient;
import com.incode.verification.client.ProviderClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@Profile("single-node")
public class ProviderResilienceConfiguration {
  @Bean
  ProviderClient freeProvider(
      @Qualifier("freeProviderClient") RestClient client, ProviderProperties properties) {
    return new FreeProviderClient(client, properties.free());
  }

  @Bean
  ProviderClient premiumProvider(
      @Qualifier("premiumProviderClient") RestClient client, ProviderProperties properties) {
    return new PremiumProviderClient(client, properties.premium());
  }
}
