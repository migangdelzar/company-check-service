package com.incode.verification.config.provider;

import com.incode.verification.config.hints.ProviderRuntimeHints;
import io.netty.channel.ChannelOption;
import java.time.Duration;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.context.annotation.Scope;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

@Configuration(proxyBeanMethods = false)
@ImportRuntimeHints(ProviderRuntimeHints.class)
public class ProviderHttpConfiguration {
  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  @ConditionalOnMissingBean(WebClient.Builder.class)
  WebClient.Builder providerWebClientBuilder() {
    return WebClient.builder();
  }

  @Bean("freeProviderClient")
  WebClient freeProviderClient(
      WebClient.Builder builder, ProviderProperties properties, ConnectionProvider providerPool) {
    return client(builder, properties.free(), providerPool, properties.pool());
  }

  @Bean("premiumProviderClient")
  WebClient premiumProviderClient(
      WebClient.Builder builder, ProviderProperties properties, ConnectionProvider providerPool) {
    return client(builder, properties.premium(), providerPool, properties.pool());
  }

  @Bean(destroyMethod = "dispose")
  ConnectionProvider providerConnectionProvider(ProviderProperties properties) {
    var pool = properties.pool();
    return ConnectionProvider.builder("company-check-provider")
        .maxConnections(pool.maxConnectionsTotal())
        .pendingAcquireTimeout(pool.connectionRequestTimeout())
        .maxIdleTime(pool.evictIdleAfter())
        .evictInBackground(Duration.ofSeconds(30))
        .build();
  }

  private WebClient client(
      WebClient.Builder builder,
      ProviderEndpointProperties endpoint,
      ConnectionProvider providerConnectionProvider,
      ProviderProperties.HttpPoolProperties pool) {
    var httpClient =
        HttpClient.create(providerConnectionProvider)
            .option(
                ChannelOption.CONNECT_TIMEOUT_MILLIS,
                Math.toIntExact(pool.connectTimeout().toMillis()))
            .responseTimeout(pool.responseTimeout());
    return builder
        .baseUrl(endpoint.baseUrl())
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .build();
  }
}
