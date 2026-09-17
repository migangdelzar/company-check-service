package com.incode.verification.repository.ratelimit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.incode.verification.config.provider.ProviderRateLimitProperties;
import com.incode.verification.service.model.ProviderType;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class RedisProviderRateLimiterIntegrationTest {
  @Container
  static final GenericContainer<?> REDIS =
      new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

  private LettuceConnectionFactory factory;
  private RedisProviderRateLimiter limiter;

  @BeforeEach
  void setUp() {
    factory =
        new LettuceConnectionFactory(
            new RedisStandaloneConfiguration(REDIS.getHost(), REDIS.getMappedPort(6379)));
    factory.afterPropertiesSet();
    var template = new StringRedisTemplate(factory);
    template.afterPropertiesSet();
    limiter =
        new RedisProviderRateLimiter(
            template,
            new ProviderRateLimitProperties(
                new ProviderRateLimitProperties.Limit(2, Duration.ofSeconds(10)),
                new ProviderRateLimitProperties.Limit(1, Duration.ofSeconds(10)),
                "integration:rate:"));
  }

  @AfterEach
  void tearDown() {
    factory.destroy();
  }

  @Test
  void sharesTheProviderBudgetInRedis() {
    assertTrue(limiter.tryAcquire(ProviderType.FREE));
    assertTrue(limiter.tryAcquire(ProviderType.FREE));
    assertFalse(limiter.tryAcquire(ProviderType.FREE));
    assertTrue(limiter.tryAcquire(ProviderType.PREMIUM));
  }
}
