package com.incode.verification.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incode.verification.service.model.NormalizedQuery;
import com.incode.verification.service.model.Verification;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactoryOptions;
import java.time.Instant;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.r2dbc.core.DatabaseClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class R2dbcVerificationRepositoryIntegrationTest {
  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

  private R2dbcVerificationRepository repository;

  @BeforeEach
  void setUp() {
    DataSource dataSource =
        new DriverManagerDataSource(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    new ResourceDatabasePopulator(
            new ClassPathResource("db/migration/V1__create_verifications.sql"))
        .execute(dataSource);
    var factory =
        ConnectionFactories.get(
            ConnectionFactoryOptions.builder()
                .option(ConnectionFactoryOptions.DRIVER, "postgresql")
                .option(ConnectionFactoryOptions.HOST, POSTGRES.getHost())
                .option(ConnectionFactoryOptions.PORT, POSTGRES.getMappedPort(5432))
                .option(ConnectionFactoryOptions.DATABASE, POSTGRES.getDatabaseName())
                .option(ConnectionFactoryOptions.USER, POSTGRES.getUsername())
                .option(ConnectionFactoryOptions.PASSWORD, POSTGRES.getPassword())
                .build());
    repository =
        new R2dbcVerificationRepository(DatabaseClient.create(factory), new ObjectMapper());
  }

  @Test
  void persistsAndReadsInProgressVerification() {
    var now = Instant.parse("2026-01-01T00:00:00Z");
    var verification =
        Verification.start(
            UUID.randomUUID(), "Acme", new NormalizedQuery("ACME"), now, now.plusSeconds(600));

    assertTrue(repository.insertInProgress(verification).block());
    assertEquals(verification, repository.findById(verification.id()).block());
  }
}
