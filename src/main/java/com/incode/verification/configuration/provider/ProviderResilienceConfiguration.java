package com.incode.verification.configuration.provider;

import com.incode.verification.adapter.out.provider.FreeProvider;
import com.incode.verification.adapter.out.provider.PremiumProvider;
import com.incode.verification.adapter.out.provider.RestClientProviderAdapter;
import com.incode.verification.application.port.out.ProviderLookupPort;
import com.incode.verification.domain.provider.ProviderType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@Profile("single-node")
public class ProviderResilienceConfiguration {
  @Bean
  ProviderLookupPort freeProvider(
      @Qualifier("freeProviderClient") RestClient client, ProviderProperties properties) {
    return new FreeProvider(
        new RestClientProviderAdapter(client, ProviderType.FREE, properties.free()));
  }

  @Bean
  ProviderLookupPort premiumProvider(
      @Qualifier("premiumProviderClient") RestClient client, ProviderProperties properties) {
    return new PremiumProvider(
        new RestClientProviderAdapter(client, ProviderType.PREMIUM, properties.premium()));
  }
}
