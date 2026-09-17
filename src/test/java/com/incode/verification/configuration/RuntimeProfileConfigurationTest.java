package com.incode.verification.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incode.verification.adapter.out.coordination.LocalCoordinationAdapter;
import com.incode.verification.adapter.out.coordination.LocalExpirationLock;
import com.incode.verification.adapter.out.coordination.RedisCoordinationAdapter;
import com.incode.verification.adapter.out.coordination.RedisExpirationLock;
import com.incode.verification.adapter.out.ratelimit.ProviderRateLimiter;
import com.incode.verification.adapter.out.ratelimit.RedisInboundRateLimiter;
import com.incode.verification.adapter.out.ratelimit.RedisProviderRateLimiter;
import com.incode.verification.adapter.out.ratelimit.Resilience4jInboundRateLimiter;
import com.incode.verification.application.port.out.CoordinationPort;
import com.incode.verification.application.port.out.ExpirationLock;
import com.incode.verification.application.port.out.InboundRateLimiter;
import com.incode.verification.application.port.out.ProviderLookupPort;
import com.incode.verification.configuration.coordination.CoordinationConfiguration;
import com.incode.verification.configuration.coordination.CoordinationProperties;
import com.incode.verification.configuration.coordination.ExpirationLockConfiguration;
import com.incode.verification.configuration.coordination.ExpirationLockProperties;
import com.incode.verification.configuration.coordination.LocalCoordinationConfiguration;
import com.incode.verification.configuration.provider.DistributedProviderResilienceConfiguration;
import com.incode.verification.configuration.provider.ProviderEndpointProperties;
import com.incode.verification.configuration.provider.ProviderProperties;
import com.incode.verification.configuration.provider.ProviderRateLimitProperties;
import com.incode.verification.configuration.provider.ProviderResilienceConfiguration;
import com.incode.verification.configuration.web.InboundRateLimitConfiguration;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.client.RestClient;

class RuntimeProfileConfigurationTest {
  @Test
  void singleNodeProfileUsesLocalCoordinationAndResilience4j() {
    new ApplicationContextRunner()
        .withUserConfiguration(
            LocalBeans.class,
            ProviderBeans.class,
            LocalCoordinationConfiguration.class,
            ExpirationLockConfiguration.class,
            CoordinationConfiguration.class,
            InboundRateLimitConfiguration.class,
            ProviderResilienceConfiguration.class,
            DistributedProviderResilienceConfiguration.class)
        .withPropertyValues(
            "spring.profiles.active=single-node",
            "verification.inbound-rate-limiting.limit-for-period=100",
            "verification.inbound-rate-limiting.refresh-period=1s",
            "verification.inbound-rate-limiting.key-prefix=test:inbound:")
        .run(
            context -> {
              assertInstanceOf(
                  LocalCoordinationAdapter.class, context.getBean(CoordinationPort.class));
              assertTrue(context.getBeansOfType(RedisCoordinationAdapter.class).isEmpty());
              assertTrue(context.getBeansOfType(ProviderRateLimiter.class).isEmpty());
              assertInstanceOf(
                  Resilience4jInboundRateLimiter.class, context.getBean(InboundRateLimiter.class));
              assertInstanceOf(LocalExpirationLock.class, context.getBean(ExpirationLock.class));
              assertTrue(context.getBeansOfType(RedisInboundRateLimiter.class).isEmpty());
              assertEquals(Set.of("FreeProvider", "PremiumProvider"), providerTypes(context));
            });
  }

  @Test
  void distributedProfileUsesRedisCoordinationAndRedisRateLimiting() {
    new ApplicationContextRunner()
        .withUserConfiguration(
            DistributedBeans.class,
            ProviderBeans.class,
            CoordinationConfiguration.class,
            LocalCoordinationConfiguration.class,
            ExpirationLockConfiguration.class,
            InboundRateLimitConfiguration.class,
            ProviderResilienceConfiguration.class,
            DistributedProviderResilienceConfiguration.class)
        .withPropertyValues(
            "spring.profiles.active=distributed",
            "verification.inbound-rate-limiting.limit-for-period=100",
            "verification.inbound-rate-limiting.refresh-period=1s",
            "verification.inbound-rate-limiting.key-prefix=test:inbound:")
        .run(
            context -> {
              assertInstanceOf(
                  RedisCoordinationAdapter.class, context.getBean(CoordinationPort.class));
              assertTrue(context.getBeansOfType(LocalCoordinationAdapter.class).isEmpty());
              assertInstanceOf(
                  RedisProviderRateLimiter.class, context.getBean(ProviderRateLimiter.class));
              assertInstanceOf(
                  RedisInboundRateLimiter.class, context.getBean(InboundRateLimiter.class));
              assertInstanceOf(RedisExpirationLock.class, context.getBean(ExpirationLock.class));
              assertTrue(context.getBeansOfType(Resilience4jInboundRateLimiter.class).isEmpty());
              assertEquals(
                  Set.of("DistributedFreeProvider", "DistributedPremiumProvider"),
                  providerTypes(context));
            });
  }

  @Test
  void singleNodeConfigurationExcludesRedisAutoConfiguration() throws IOException {
    var source =
        new YamlPropertySourceLoader()
            .load("single-node", new ClassPathResource("application-single-node.yml")).stream()
                .findFirst()
                .orElseThrow();

    assertEquals(
        "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration",
        source.getProperty("spring.autoconfigure.exclude[0]"));
    assertEquals(
        "org.springframework.boot.data.redis.autoconfigure.DataRedisRepositoriesAutoConfiguration",
        source.getProperty("spring.autoconfigure.exclude[1]"));
  }

  @Test
  void distributedProfileEnablesTheSharedRedisConnectionPool() throws IOException {
    var source =
        new YamlPropertySourceLoader()
            .load("distributed", new ClassPathResource("application-distributed.yml")).stream()
                .findFirst()
                .orElseThrow();

    assertEquals(true, source.getProperty("spring.data.redis.lettuce.pool.enabled"));
    assertEquals(32, source.getProperty("spring.data.redis.lettuce.pool.max-active"));
    assertEquals(16, source.getProperty("spring.data.redis.lettuce.pool.max-idle"));
    assertEquals(4, source.getProperty("spring.data.redis.lettuce.pool.min-idle"));
    assertEquals("100ms", source.getProperty("spring.data.redis.lettuce.pool.max-wait"));
    assertEquals("250ms", source.getProperty("spring.data.redis.timeout"));
    assertEquals("100ms", source.getProperty("spring.data.redis.connect-timeout"));
  }

  @Test
  void defaultProfileUsesBoundedJdbcConnectionPool() throws IOException {
    var source =
        new YamlPropertySourceLoader()
            .load("application", new ClassPathResource("application.yml")).stream()
                .findFirst()
                .orElseThrow();

    assertEquals(16, source.getProperty("spring.datasource.hikari.maximum-pool-size"));
    assertEquals(4, source.getProperty("spring.datasource.hikari.minimum-idle"));
    assertEquals(250, source.getProperty("spring.datasource.hikari.connection-timeout"));
    assertEquals(250, source.getProperty("spring.datasource.hikari.validation-timeout"));
    assertEquals(600000, source.getProperty("spring.datasource.hikari.idle-timeout"));
    assertEquals(1500000, source.getProperty("spring.datasource.hikari.max-lifetime"));
    assertEquals(true, source.getProperty("spring.threads.virtual.enabled"));
    assertEquals(true, source.getProperty("spring.main.keep-alive"));
    assertEquals(
        "company-check:expiration:lock", source.getProperty("verification.expiration.lock.key"));
    assertEquals("30s", source.getProperty("verification.expiration.lock.ttl"));
  }

  private Set<String> providerTypes(ApplicationContext context) {
    return context.getBeansOfType(ProviderLookupPort.class).values().stream()
        .map(bean -> bean.getClass().getSimpleName())
        .collect(Collectors.toSet());
  }

  @Configuration(proxyBeanMethods = false)
  static class LocalBeans {
    @Bean
    ExpirationLockProperties expirationLockProperties() {
      return new ExpirationLockProperties("test:expiration", Duration.ofSeconds(30));
    }

    @Bean
    CacheManager cacheManager() {
      return new ConcurrentMapCacheManager("verification");
    }

    @Bean
    RateLimiterRegistry rateLimiterRegistry() {
      return RateLimiterRegistry.ofDefaults();
    }
  }

  @Configuration(proxyBeanMethods = false)
  static class ProviderBeans {
    @Bean("freeProviderClient")
    RestClient freeProviderClient() {
      return RestClient.create();
    }

    @Bean("premiumProviderClient")
    RestClient premiumProviderClient() {
      return RestClient.create();
    }

    @Bean
    ProviderProperties providerProperties() {
      return new ProviderProperties(
          new ProviderEndpointProperties("http://free.test", "/verify?query={query}", ""),
          new ProviderEndpointProperties("http://premium.test", "/verify?query={query}", ""),
          Duration.ofMillis(500));
    }
  }

  @Configuration(proxyBeanMethods = false)
  static class DistributedBeans {
    @Bean
    ExpirationLockProperties expirationLockProperties() {
      return new ExpirationLockProperties("test:expiration", Duration.ofSeconds(30));
    }

    @Bean
    CacheManager cacheManager() {
      return new ConcurrentMapCacheManager("verification");
    }

    @Bean
    ObjectMapper objectMapper() {
      return new ObjectMapper();
    }

    @Bean
    CoordinationProperties coordinationProperties() {
      return new CoordinationProperties(
          100,
          Duration.ofMinutes(1),
          Duration.ofMinutes(1),
          Duration.ofMinutes(1),
          Duration.ZERO,
          Duration.ofSeconds(1),
          Duration.ofMillis(10),
          1,
          "test:");
    }

    @Bean
    StringRedisTemplate redis() {
      return org.mockito.Mockito.mock(StringRedisTemplate.class);
    }

    @Bean
    ProviderRateLimitProperties providerRateLimitProperties() {
      return new ProviderRateLimitProperties(
          new ProviderRateLimitProperties.Limit(100, Duration.ofSeconds(1)),
          new ProviderRateLimitProperties.Limit(100, Duration.ofSeconds(1)),
          "test:");
    }
  }
}
