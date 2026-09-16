package com.incode.verification.adapter.config;

import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

@Configuration(proxyBeanMethods = false)
public class PersistenceConfiguration {
  @Bean
  JdbcClient jdbcClient(DataSource dataSource) {
    return JdbcClient.create(dataSource);
  }
}
