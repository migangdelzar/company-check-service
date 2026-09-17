package com.incode.verification.domain;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incode.CompanyCheckApplication;
import com.incode.verification.application.port.out.VerificationRepository;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.client.RestClient;

@ApplicationModuleTest(classes = CompanyCheckApplication.class, module = "verification")
@TestPropertySource(
    properties = {
        "spring.main.web-application-type=none",
        "spring.autoconfigure.exclude="
            + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
            + "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration,"
            + "org.springframework.boot.data.redis.autoconfigure.DataRedisRepositoriesAutoConfiguration,"
            + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"})
class DomainApplicationModuleTest {
  @Autowired private ApplicationContext context;

  @MockitoBean private DataSource dataSource;

  @MockitoBean private ObjectMapper objectMapper;

  @MockitoBean private VerificationRepository verificationRepository;

  @MockitoBean private PlatformTransactionManager transactionManager;

  @MockitoBean(name = "freeProviderClient") private RestClient freeProviderClient;

  @MockitoBean(name = "premiumProviderClient") private RestClient premiumProviderClient;

  @Test
  void bootsTheDomainModuleInIsolation() {
    assertNotNull(context);
  }
}
