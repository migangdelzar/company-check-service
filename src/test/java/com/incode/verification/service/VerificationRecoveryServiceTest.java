package com.incode.verification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.incode.verification.repository.CoordinationRepository;
import com.incode.verification.repository.VerificationRepository;
import com.incode.verification.service.model.Company;
import com.incode.verification.service.model.NormalizedQuery;
import com.incode.verification.service.model.ProviderResult;
import com.incode.verification.service.model.ProviderType;
import com.incode.verification.service.model.Verification;
import com.incode.verification.service.model.VerificationResult;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class VerificationRecoveryServiceTest {
  private final VerificationRepository repository = mock(VerificationRepository.class);
  private final CoordinationRepository coordination = mock(CoordinationRepository.class);
  private final VerificationStoreService store = mock(VerificationStoreService.class);
  private final VerificationRecoveryService recovery =
      new VerificationRecoveryService(repository, coordination, store);

  @Test
  void reconcilesACompletedCachedResult() {
    var verification = inProgress();
    var cached = VerificationResult.from(completed(verification.id(), "ACME"));
    when(coordination.get(verification.query())).thenReturn(Mono.just(cached));
    when(store.store(any(Verification.class), any(NormalizedQuery.class)))
        .thenReturn(Mono.just(cached));

    assertEquals(cached, recovery.cached(verification).block());
  }

  @Test
  void fallsBackToTheSharedDatabaseResultWhenCacheMisses() {
    var verification = inProgress();
    var shared = completed(verification.id(), "ACME");
    var result = VerificationResult.from(shared);
    when(coordination.get(verification.query())).thenReturn(Mono.empty());
    when(repository.findByQuery(verification.query())).thenReturn(Mono.just(shared));
    when(store.store(any(Verification.class), any(NormalizedQuery.class)))
        .thenReturn(Mono.just(result));

    assertEquals(result, recovery.recover(verification).block());
  }

  private static Verification inProgress() {
    return Verification.start(
        UUID.randomUUID(),
        "ACME",
        new NormalizedQuery("ACME"),
        Instant.EPOCH,
        Instant.EPOCH.plusSeconds(600));
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
