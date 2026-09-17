package com.incode.verification.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incode.verification.service.model.NormalizedQuery;
import com.incode.verification.service.model.Verification;
import java.time.Instant;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class JdbcVerificationRepositoryIntegrationTest {
  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

  private JdbcVerificationRepository repository;

  @BeforeEach
  void setUp() {
    DataSource dataSource =
        new DriverManagerDataSource(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    new ResourceDatabasePopulator(
            new ClassPathResource("db/migration/V1__create_verifications.sql"))
        .execute(dataSource);
    repository = new JdbcVerificationRepository(JdbcClient.create(dataSource), new ObjectMapper());
  }

  @Test
  void persistsAndReadsInProgressVerification() {
    var now = Instant.parse("2026-01-01T00:00:00Z");
    var verification =
        Verification.start(
            UUID.randomUUID(), "Acme", new NormalizedQuery("ACME"), now, now.plusSeconds(600));

    assertTrue(repository.insertInProgress(verification));
    assertEquals(verification, repository.findById(verification.id()).orElseThrow());
  }
}
