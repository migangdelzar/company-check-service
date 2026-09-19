package com.incode.verification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.incode.verification.config.persistence.DatabaseRetryProperties;
import com.incode.verification.repository.CoordinationRepository;
import com.incode.verification.repository.VerificationRepository;
import com.incode.verification.service.model.Company;
import com.incode.verification.service.model.NormalizedQuery;
import com.incode.verification.service.model.ProviderResult;
import com.incode.verification.service.model.ProviderType;
import com.incode.verification.service.model.Verification;
import com.incode.verification.service.model.VerificationResult;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

class VerificationStoreServiceTest {
  private final VerificationRepository repository = mock(VerificationRepository.class);
  private final CoordinationRepository coordination = mock(CoordinationRepository.class);
  private final TransactionalOperator transaction = mock(TransactionalOperator.class);
  private VerificationStoreService store;

  @BeforeEach
  void setUp() {
    when(transaction.transactional(any(Mono.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    store =
        new VerificationStoreService(
            repository,
            coordination,
            transaction,
            new DatabaseRetryProperties(1, Duration.ofMillis(1), 2, Duration.ofMillis(2)));
  }

  @Test
  void claimsCompletesAndPublishesAfterTheReactiveTransaction() {
    var verification = completed(UUID.randomUUID(), "ACME");
    var query = verification.query();
    var token = UUID.randomUUID();
    var result = VerificationResult.from(verification);
    when(repository.claim(verification.id())).thenReturn(Mono.just(token));
    when(repository.complete(verification.id(), token, verification)).thenReturn(Mono.just(true));
    when(coordination.put(query, result)).thenReturn(Mono.just(result));

    assertEquals(result, store.store(verification, query).block());
    verify(coordination).put(query, result);
  }

  @Test
  void reloadsWhenTheClaimIsAlreadyOwned() {
    var verification = completed(UUID.randomUUID(), "ACME");
    var result = VerificationResult.from(verification);
    when(repository.claim(verification.id())).thenReturn(Mono.empty());
    when(repository.findById(verification.id())).thenReturn(Mono.just(verification));
    when(coordination.put(verification.query(), result)).thenReturn(Mono.just(result));

    assertEquals(result, store.store(verification, verification.query()).block());
  }

  private static Verification completed(UUID id, String query) {
    return Verification.start(
            id, query, new NormalizedQuery(query), Instant.EPOCH, Instant.EPOCH.plusSeconds(600))
        .complete(
            new ProviderResult.Success(
                List.of(new Company("1", "Acme", LocalDate.EPOCH, "address", true)),
                ProviderType.FREE));
  }
}
