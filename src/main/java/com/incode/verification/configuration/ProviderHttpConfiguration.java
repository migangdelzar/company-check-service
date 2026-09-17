package com.incode.verification.configuration;

import com.incode.verification.adapter.out.provider.ProviderEndpointProperties;
import com.incode.verification.adapter.out.provider.ProviderProperties;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
public class ProviderHttpConfiguration {
  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  @ConditionalOnMissingBean(RestClient.Builder.class)
  RestClient.Builder providerRestClientBuilder() {
    return RestClient.builder();
  }

  @Bean("freeProviderClient")
  RestClient freeProviderClient(
      RestClient.Builder builder, ProviderProperties properties, CloseableHttpClient httpClient) {
    return client(builder, properties.free(), httpClient);
  }

  @Bean("premiumProviderClient")
  RestClient premiumProviderClient(
      RestClient.Builder builder, ProviderProperties properties, CloseableHttpClient httpClient) {
    return client(builder, properties.premium(), httpClient);
  }

  @Bean(destroyMethod = "close")
  CloseableHttpClient providerHttpClient(ProviderProperties properties) {
    var pool = properties.pool();
    var responseTimeout = Timeout.ofMilliseconds(pool.responseTimeout().toMillis());
    var connectionConfig =
        ConnectionConfig.custom()
            .setConnectTimeout(Timeout.ofMilliseconds(pool.connectTimeout().toMillis()))
            .setSocketTimeout(responseTimeout)
            .setValidateAfterInactivity(
                TimeValue.ofMilliseconds(pool.validateAfterInactivity().toMillis()))
            .build();
    var manager =
        PoolingHttpClientConnectionManagerBuilder.create()
            .setMaxConnTotal(pool.maxConnectionsTotal())
            .setMaxConnPerRoute(pool.maxConnectionsPerRoute())
            .setDefaultConnectionConfig(connectionConfig)
            .build();
    var requestConfig =
        RequestConfig.custom()
            .setConnectionRequestTimeout(
                Timeout.ofMilliseconds(pool.connectionRequestTimeout().toMillis()))
            .setResponseTimeout(responseTimeout)
            .build();
    return HttpClients.custom()
        .setConnectionManager(manager)
        .setDefaultRequestConfig(requestConfig)
        .evictExpiredConnections()
        .evictIdleConnections(TimeValue.ofMilliseconds(pool.evictIdleAfter().toMillis()))
        .build();
  }

  private RestClient client(
      RestClient.Builder builder,
      ProviderEndpointProperties endpoint,
      CloseableHttpClient httpClient) {
    return builder
        .baseUrl(endpoint.baseUrl())
        .requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
        .build();
  }
}
