package com.incode.verification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.incode.verification.client.ProviderClient;
import com.incode.verification.exception.CoordinationUnavailableException;
import com.incode.verification.exception.ProviderSubmissionException;
import com.incode.verification.exception.domain.VerificationConflictException;
import com.incode.verification.exception.domain.VerificationNotFoundException;
import com.incode.verification.repository.CoordinationRepository;
import com.incode.verification.repository.VerificationRepository;
import com.incode.verification.service.model.ProviderFailure;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

class VerificationServiceTest {
  private final Instant now = Instant.parse("2026-01-01T00:00:00Z");

  @Test
  void persistsInProgressBeforeProviderAndExposesImmutableView() {
    var repository = new MemoryRepository();
    var provider =
        (ProviderClient)
            q ->
                new ProviderResult.Success(
                    List.of(
                        new com.incode.verification.service.model.Company(
                            "A", "A", LocalDate.parse("2020-01-01"), "x", true)));
    var service =
        services(
            repository,
            new NoopCoordination(),
            provider,
            provider,
            Clock.fixed(now, ZoneOffset.UTC),
            Duration.ofMinutes(1));
    VerificationResult view =
        service.start(new StartVerificationCommand(UUID.randomUUID(), " acme "));
    assertEquals(VerificationStatus.COMPLETED, view.status());
    assertThrows(
        UnsupportedOperationException.class, () -> view.otherResults().add(view.company()));
  }

  @Test
  void fallsBackOnlyForDomainFallbackFailures() {
    var repository = new MemoryRepository();
    var calls = new ArrayList<String>();
    var primary =
        (ProviderClient)
            q -> {
              calls.add("primary");
              return new ProviderResult.Failure(new ProviderFailure.Unavailable());
            };
    var fallback =
        (ProviderClient)
            q -> {
              calls.add("fallback");
              return new ProviderResult.Failure(new ProviderFailure.Timeout());
            };
    var service =
        services(
            repository,
            new NoopCoordination(),
            primary,
            fallback,
            Clock.fixed(now, ZoneOffset.UTC),
            Duration.ofMinutes(1));
    assertThrows(
        ProviderSubmissionException.class,
        () -> service.start(new StartVerificationCommand(UUID.randomUUID(), "acme")));
    assertEquals(List.of("primary", "fallback"), calls);
  }

  @Test
  void replaysTerminalVerificationAndRejectsIdReuse() {
    var repository = new MemoryRepository();
    var id = UUID.randomUUID();
    var verification =
        Verification.start(
                id,
                " Acme ",
                new com.incode.verification.service.model.NormalizedQuery("ACME"),
                now,
                now.plusSeconds(60))
            .complete(
                new ProviderResult.Success(
                    List.of(
                        new com.incode.verification.service.model.Company(
                            "A", "A", LocalDate.parse("2020-01-01"), "x", true)),
                    ProviderType.FREE));
    repository.values.put(id, verification);
    var providerCalls = new ArrayList<String>();
    var provider =
        (ProviderClient)
            q -> {
              providerCalls.add(q.value());
              return new ProviderResult.Success(List.of());
            };
    var service = service(repository, provider, provider);

    assertEquals(
        VerificationStatus.COMPLETED,
        service.start(new StartVerificationCommand(id, "acme")).status());
    assertThrows(
        VerificationConflictException.class,
        () -> service.start(new StartVerificationCommand(id, "other")));
    assertEquals(List.of(), providerCalls);
  }

  @Test
  void returnsConflictForExistingInProgressVerification() {
    var repository = new MemoryRepository();
    var id = UUID.randomUUID();
    repository.values.put(
        id,
        Verification.start(
            id,
            "acme",
            new com.incode.verification.service.model.NormalizedQuery("ACME"),
            now,
            now.plusSeconds(60)));
    var service =
        service(
            repository,
            q -> new ProviderResult.Success(List.of()),
            q -> new ProviderResult.Success(List.of()));
    var error =
        assertThrows(
            VerificationConflictException.class,
            () -> service.start(new StartVerificationCommand(id, "ACME")));
    assertEquals("VERIFICATION_IN_PROGRESS", error.code());
  }

  @Test
  void completesFromSharedCacheWithoutCallingProviders() {
    var repository = new MemoryRepository();
    var cached =
        new VerificationResult(
            UUID.randomUUID(),
            "ACME",
            "ACME",
            now,
            now.plusSeconds(60),
            VerificationStatus.COMPLETED,
            new com.incode.verification.service.model.Company(
                "A", "A", LocalDate.parse("2020-01-01"), "x", true),
            List.of(),
            ProviderType.PREMIUM,
            null);
    var service =
        services(
            repository,
            new CachedCoordination(cached),
            q -> {
              throw new AssertionError("provider must not be called");
            },
            q -> {
              throw new AssertionError("provider must not be called");
            },
            Clock.fixed(now, ZoneOffset.UTC),
            Duration.ofMinutes(1));
    assertEquals(
        VerificationStatus.COMPLETED,
        service.start(new StartVerificationCommand(UUID.randomUUID(), "ACME")).status());
  }

  @Test
  void returnsInProgressWhenAnotherLeaseOwnerHasNoResultYet() {
    var repository = new MemoryRepository();
    var service =
        services(
            repository,
            new WaitingCoordination(),
            q -> {
              throw new AssertionError("provider must not be called");
            },
            q -> {
              throw new AssertionError("provider must not be called");
            },
            Clock.fixed(now, ZoneOffset.UTC),
            Duration.ofMinutes(1));
    assertEquals(
        VerificationStatus.IN_PROGRESS,
        service.start(new StartVerificationCommand(UUID.randomUUID(), "ACME")).status());
  }

  @Test
  void refusesProviderLookupWhenCoordinationIsUnavailable() {
    var repository = new MemoryRepository();
    var service =
        services(
            repository,
            new UnavailableCoordination(),
            providerMustNotRun(),
            providerMustNotRun(),
            Clock.fixed(now, ZoneOffset.UTC),
            Duration.ofMinutes(1));

    assertThrows(
        CoordinationUnavailableException.class,
        () -> service.start(new StartVerificationCommand(UUID.randomUUID(), "ACME")));
  }

  @Test
  void pollingGetHydratesInProgressVerificationFromSharedTerminalCache() {
    var repository = new MemoryRepository();
    var id = UUID.randomUUID();
    repository.values.put(
        id,
        Verification.start(
            id,
            "acme",
            new com.incode.verification.service.model.NormalizedQuery("ACME"),
            now,
            now.plusSeconds(60)));
    var company =
        new com.incode.verification.service.model.Company(
            "A", "A", LocalDate.parse("2020-01-01"), "x", true);
    var cached =
        new VerificationResult(
            id,
            "acme",
            "ACME",
            now,
            now.plusSeconds(60),
            VerificationStatus.COMPLETED,
            company,
            List.of(),
            ProviderType.FREE,
            null);
    var service =
        services(
            repository,
            new CachedCoordination(cached),
            q -> new ProviderResult.Success(List.of(), ProviderType.FREE),
            q -> new ProviderResult.Success(List.of(), ProviderType.FREE),
            Clock.fixed(now, ZoneOffset.UTC),
            Duration.ofMinutes(1));

    assertEquals(VerificationStatus.COMPLETED, service.get(id).status());
    assertInstanceOf(
        com.incode.verification.service.model.VerificationState.Completed.class,
        repository.values.get(id).state());
  }

  @Test
  void sharedCachedFailureRemainsFailure() {
    var repository = new MemoryRepository();
    var failure = new ProviderFailure.Timeout();
    var cached =
        new VerificationResult(
            UUID.randomUUID(),
            "ACME",
            "ACME",
            now,
            now.plusSeconds(60),
            VerificationStatus.FAILED,
            null,
            List.of(),
            null,
            failure);
    var service =
        services(
            repository,
            new CachedCoordination(cached),
            q -> {
              throw new AssertionError("provider must not be called");
            },
            q -> {
              throw new AssertionError("provider must not be called");
            },
            Clock.fixed(now, ZoneOffset.UTC),
            Duration.ofMinutes(1));

    var error =
        assertThrows(
            ProviderSubmissionException.class,
            () -> service.start(new StartVerificationCommand(UUID.randomUUID(), "ACME")));
    assertEquals("PROVIDERS_UNAVAILABLE", error.code());
  }

  @Test
  void hydratesAnotherVerificationFromSharedTerminalSuccess() {
    var repository = new MemoryRepository();
    var shared = completed(UUID.randomUUID(), "ACME");
    repository.terminals.put("ACME", shared);
    var service = service(repository, providerMustNotRun(), providerMustNotRun());

    var id = UUID.randomUUID();
    var result = service.start(new StartVerificationCommand(id, "acme"));

    assertEquals(VerificationStatus.COMPLETED, result.status());
    assertEquals(shared.state(), repository.findById(id).orElseThrow().state());
  }

  @Test
  void preservesSharedTerminalFailureWhenHydratingAnotherVerification() {
    var repository = new MemoryRepository();
    var failed =
        Verification.start(
                UUID.randomUUID(),
                "ACME",
                new com.incode.verification.service.model.NormalizedQuery("ACME"),
                now,
                now.plusSeconds(60))
            .fail(new ProviderFailure.Timeout());
    repository.terminals.put("ACME", failed);
    var service = service(repository, providerMustNotRun(), providerMustNotRun());

    var result = service.start(new StartVerificationCommand(UUID.randomUUID(), "acme"));

    assertEquals(VerificationStatus.FAILED, result.status());
    assertInstanceOf(ProviderFailure.Timeout.class, result.failure());
  }

  @Test
  void pollingHydratesSharedTerminalStateForAnInProgressVerification() {
    var repository = new MemoryRepository();
    var id = UUID.randomUUID();
    repository.values.put(
        id,
        Verification.start(
            id,
            "ACME",
            new com.incode.verification.service.model.NormalizedQuery("ACME"),
            now,
            now.plusSeconds(60)));
    var shared = completed(UUID.randomUUID(), "ACME");
    repository.terminals.put("ACME", shared);
    var service = service(repository, providerMustNotRun(), providerMustNotRun());

    var result = service.get(id);

    assertEquals(VerificationStatus.COMPLETED, result.status());
  }

  private Verification completed(UUID id, String query) {
    return Verification.start(
            id,
            query,
            new com.incode.verification.service.model.NormalizedQuery(query),
            now,
            now.plusSeconds(60))
        .complete(new ProviderResult.Success(List.of(company()), ProviderType.FREE));
  }

  private com.incode.verification.service.model.Company company() {
    return new com.incode.verification.service.model.Company(
        "A", "A", LocalDate.parse("2020-01-01"), "x", true);
  }

  private ProviderClient providerMustNotRun() {
    return q -> {
      throw new AssertionError("provider must not be called");
    };
  }

  @Test
  void missingVerificationIsNotFound() {
    var repository = new MemoryRepository();
    var service =
        service(
            repository,
            q -> new ProviderResult.Success(List.of()),
            q -> new ProviderResult.Success(List.of()));
    assertInstanceOf(
        VerificationNotFoundException.class,
        assertThrows(VerificationNotFoundException.class, () -> service.get(UUID.randomUUID())));
  }

  private Services service(
      MemoryRepository repository, ProviderClient primary, ProviderClient fallback) {
    return services(repository, new NoopCoordination(), primary, fallback);
  }

  private Services services(
      MemoryRepository repository,
      CoordinationRepository coordination,
      ProviderClient primary,
      ProviderClient fallback) {
    return services(
        repository,
        coordination,
        primary,
        fallback,
        Clock.fixed(now, ZoneOffset.UTC),
        Duration.ofMinutes(1));
  }

  private Services services(
      MemoryRepository repository,
      CoordinationRepository coordination,
      ProviderClient primary,
      ProviderClient fallback,
      Clock clock,
      Duration lifetime) {
    var terminal =
        new VerificationStoreService(
            repository,
            coordination,
            new RetryTemplate(),
            new TransactionTemplate(noOpTransactions()));
    var recovery = new VerificationRecoveryService(repository, coordination, terminal);
    return new Services(
        new VerificationService(
            repository,
            coordination,
            new ProviderService(primary, fallback),
            terminal,
            recovery,
            clock,
            lifetime));
  }

  private record Services(VerificationService service) {
    VerificationResult start(StartVerificationCommand command) {
      return service.start(command);
    }

    VerificationResult get(UUID verificationId) {
      return service.get(verificationId);
    }
  }

  private static PlatformTransactionManager noOpTransactions() {
    return new PlatformTransactionManager() {
      @Override
      public TransactionStatus getTransaction(TransactionDefinition definition) {
        return new SimpleTransactionStatus();
      }

      @Override
      public void commit(TransactionStatus status) {}

      @Override
      public void rollback(TransactionStatus status) {}
    };
  }

  private static final class MemoryRepository implements VerificationRepository {
    private final Map<UUID, Verification> values = new HashMap<>();
    private final Map<String, Verification> terminals = new HashMap<>();

    @Override
    public boolean insertInProgress(Verification v) {
      return values.putIfAbsent(v.id(), v) == null;
    }

    @Override
    public Optional<Verification> findById(UUID id) {
      return Optional.ofNullable(values.get(id));
    }

    @Override
    public Optional<Verification> findByQuery(
        com.incode.verification.service.model.NormalizedQuery query) {
      return Optional.ofNullable(terminals.get(query.value()));
    }

    @Override
    public UUID claim(UUID id) {
      return values.containsKey(id) ? UUID.randomUUID() : null;
    }

    @Override
    public boolean complete(UUID id, UUID token, Verification v) {
      values.put(id, v);
      return true;
    }
  }

  private static class NoopCoordination implements CoordinationRepository {
    @Override
    public Lease acquire(com.incode.verification.service.model.NormalizedQuery query) {
      return new Lease() {
        @Override
        public boolean acquired() {
          return true;
        }

        @Override
        public void close() {}
      };
    }

    @Override
    public Optional<VerificationResult> get(
        com.incode.verification.service.model.NormalizedQuery query) {
      return Optional.empty();
    }

    @Override
    public VerificationResult put(
        com.incode.verification.service.model.NormalizedQuery query, VerificationResult result) {
      return result;
    }
  }

  private static final class CachedCoordination extends NoopCoordination {
    private final VerificationResult cached;

    CachedCoordination(VerificationResult cached) {
      this.cached = cached;
    }

    @Override
    public Lease acquire(com.incode.verification.service.model.NormalizedQuery query) {
      return new WaitingCoordination().acquire(query);
    }

    @Override
    public Optional<VerificationResult> get(
        com.incode.verification.service.model.NormalizedQuery query) {
      return Optional.of(cached);
    }
  }

  private static final class WaitingCoordination extends NoopCoordination {
    @Override
    public Lease acquire(com.incode.verification.service.model.NormalizedQuery query) {
      return new Lease() {
        @Override
        public boolean acquired() {
          return false;
        }

        @Override
        public void close() {}
      };
    }
  }

  private static final class UnavailableCoordination extends NoopCoordination {
    @Override
    public Lease acquire(com.incode.verification.service.model.NormalizedQuery query) {
      return new Lease() {
        @Override
        public boolean acquired() {
          return false;
        }

        @Override
        public boolean degraded() {
          return true;
        }

        @Override
        public void close() {}
      };
    }
  }
}
