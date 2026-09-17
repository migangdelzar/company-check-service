package com.incode;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.incode.verification.adapter.out.coordination.RedisCoordinationAdapter;
import com.incode.verification.adapter.out.ratelimit.ProviderRateLimiter;
import com.incode.verification.adapter.out.ratelimit.RedisProviderRateLimiter;
import com.incode.verification.application.port.out.CoordinationPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(
    properties = {"spring.profiles.active=distributed", "spring.task.scheduling.enabled=false"})
class CompanyCheckApplicationIntegrationTest {
  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine");

  @Container
  static final GenericContainer<?> REDIS =
      new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

  @Autowired private ApplicationContext context;

  @DynamicPropertySource
  static void containerProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.data.redis.host", REDIS::getHost);
    registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
  }

  @Test
  void startsTheDistributedApplicationAgainstPostgresAndRedis() {
    assertInstanceOf(RedisCoordinationAdapter.class, context.getBean(CoordinationPort.class));
    assertInstanceOf(RedisProviderRateLimiter.class, context.getBean(ProviderRateLimiter.class));
  }
}
