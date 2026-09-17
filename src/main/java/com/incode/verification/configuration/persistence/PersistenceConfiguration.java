package com.incode.verification.configuration.persistence;

import java.time.Duration;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration(proxyBeanMethods = false)
@EnableTransactionManagement
public class PersistenceConfiguration {
  @Bean
  JdbcClient jdbcClient(DataSource dataSource) {
    return JdbcClient.create(dataSource);
  }

  @Bean
  TransactionTemplate verificationTransactionTemplate(
      PlatformTransactionManager transactionManager, DatabaseProperties properties) {
    Duration timeout = properties.transactionTimeout();
    var template = new TransactionTemplate(transactionManager);
    template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    template.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    template.setTimeout((int) timeout.toSeconds());
    return template;
  }

  @Bean
  RetryTemplate verificationDatabaseRetryTemplate(DatabaseRetryProperties properties) {
    var policy =
        RetryPolicy.builder()
            .maxRetries(properties.maxAttempts() - 1L)
            .delay(properties.delay())
            .multiplier(properties.multiplier())
            .maxDelay(properties.maxDelay())
            .includes(TransientDataAccessException.class)
            .build();
    return new RetryTemplate(policy);
  }
}
