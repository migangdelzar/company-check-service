package com.incode.verification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.incode.verification.repository.CoordinationRepository;
import com.incode.verification.repository.VerificationRepository;
import com.incode.verification.service.model.Company;
import com.incode.verification.service.model.NormalizedQuery;
import com.incode.verification.service.model.ProviderResult;
import com.incode.verification.service.model.ProviderType;
import com.incode.verification.service.model.StartVerificationCommand;
import com.incode.verification.service.model.Verification;
import com.incode.verification.service.model.VerificationResult;
import com.incode.verification.service.model.VerificationStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class VerificationServiceTest {
  private final VerificationRepository repository = mock(VerificationRepository.class);
  private final CoordinationRepository coordination = mock(CoordinationRepository.class);
  private final ProviderService provider = mock(ProviderService.class);
  private final VerificationStoreService store = mock(VerificationStoreService.class);
  private final VerificationRecoveryService recovery = mock(VerificationRecoveryService.class);
  private final VerificationService service =
      new VerificationService(
          repository,
          coordination,
          provider,
          store,
          recovery,
          Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC),
          Duration.ofMinutes(10));

  @Test
  void returnsExistingTerminalVerificationWithoutCallingProvider() {
    var id = UUID.randomUUID();
    var verification = completed(id, "ACME");
    when(repository.findById(id)).thenReturn(Mono.just(verification));

    var result = service.start(new StartVerificationCommand(id, "ACME")).block();

    assertEquals(VerificationStatus.COMPLETED, result.status());
    verify(provider, org.mockito.Mockito.never()).resolve(any());
  }

  @Test
  void rejectsReusingAnIdForAnotherQuery() {
    var id = UUID.randomUUID();
    when(repository.findById(id)).thenReturn(Mono.just(completed(id, "ACME")));

    assertThrows(
        RuntimeException.class,
        () -> service.start(new StartVerificationCommand(id, "Other")).block());
  }

  @Test
  void createsAndStoresAReactiveProviderResult() {
    var id = UUID.randomUUID();
    var query = new NormalizedQuery("ACME");
    var completed = completed(id, "ACME");
    when(repository.findById(id)).thenReturn(Mono.empty());
    when(repository.insertInProgress(any())).thenReturn(Mono.just(true));
    when(coordination.acquire(query)).thenReturn(Mono.just(new TestLease(true, false)));
    when(repository.findByQuery(query)).thenReturn(Mono.empty());
    when(provider.resolve(query))
        .thenReturn(
            Mono.just(
                new ProviderResult.Success(
                    List.of(new Company("1", "Acme", LocalDate.EPOCH, "address", true)),
                    ProviderType.FREE)));
    when(store.store(any(Verification.class), any(NormalizedQuery.class)))
        .thenReturn(Mono.just(VerificationResult.from(completed)));

    var result = service.start(new StartVerificationCommand(id, "ACME")).block();

    assertEquals(VerificationStatus.COMPLETED, result.status());
    verify(store).store(any(Verification.class), any(NormalizedQuery.class));
  }

  @Test
  void readsAnInProgressVerificationAndAttemptsReactiveRecovery() {
    var id = UUID.randomUUID();
    var inProgress =
        Verification.start(
            id, "ACME", new NormalizedQuery("ACME"), Instant.EPOCH, Instant.EPOCH.plusSeconds(600));
    when(repository.findById(id)).thenReturn(Mono.just(inProgress));
    when(recovery.recover(inProgress)).thenReturn(Mono.empty());

    var result = service.get(id).block();

    assertEquals(VerificationStatus.IN_PROGRESS, result.status());
    verify(recovery).recover(inProgress);
  }

  private static Verification completed(UUID id, String query) {
    return Verification.start(
            id, query, new NormalizedQuery(query), Instant.EPOCH, Instant.EPOCH.plusSeconds(600))
        .complete(
            new ProviderResult.Success(
                List.of(new Company("1", "Acme", LocalDate.EPOCH, "address", true)),
                ProviderType.FREE));
  }

  private record TestLease(boolean acquired, boolean degraded)
      implements CoordinationRepository.Lease {
    @Override
    public Mono<Void> release() {
      return Mono.empty();
    }
  }
}
