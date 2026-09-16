package com.incode.verification.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incode.verification.domain.aggregate.Verification;
import com.incode.verification.domain.valueobject.NormalizedQuery;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.testcontainers.containers.PostgreSQLContainer;

class JdbcVerificationRepositoryIntegrationTest {
    @Test
    void repositoryContractIsCoveredByPostgresContainer() {
        try (var postgres = new PostgreSQLContainer<>("postgres:16-alpine")) {
            postgres.start();
            var dataSource = new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
            var repository = new JdbcVerificationRepository(JdbcClient.create(dataSource), new ObjectMapper());
            var now = Instant.parse("2026-01-01T00:00:00Z");
            var verification = Verification.start(UUID.randomUUID(), " acme ", NormalizedQuery.normalize(" acme "), now, now.plusSeconds(60));
            repository.insertInProgress(verification);
            assertTrue(repository.findById(verification.id()).isPresent());
            var token = repository.claim(verification.id());
            assertNotNull(token);
            assertFalse(repository.updateTerminal(verification.id(), UUID.randomUUID(), verification));
        }
    }
}
