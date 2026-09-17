package com.incode.verification.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.incode.verification.application.context.ExecutionContext;
import com.incode.verification.application.port.in.StartVerificationUseCase.StartVerificationCommand;
import com.incode.verification.application.port.out.CoordinationPort;
import com.incode.verification.application.port.out.ProviderLookupPort;
import com.incode.verification.application.port.out.VerificationRepository;
import com.incode.verification.application.port.out.VerificationView;
import com.incode.verification.application.service.CoordinationUnavailableException;
import com.incode.verification.application.service.ProviderSubmissionException;
import com.incode.verification.application.service.VerificationApplicationService;
import com.incode.verification.domain.aggregate.Verification;
import com.incode.verification.domain.type.ProviderFailure;
import com.incode.verification.domain.type.ProviderLookupResult;
import com.incode.verification.domain.type.ProviderType;
import com.incode.verification.domain.type.VerificationStatus;
import com.incode.verification.domain.valueobject.LookupKey;
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

class VerificationApplicationServiceTest {
  private final Instant now = Instant.parse("2026-01-01T00:00:00Z");

  @Test
  void persistsInProgressBeforeProviderAndExposesImmutableView() {
    var repository = new MemoryRepository();
    var provider =
        (ProviderLookupPort)
            (q, c) ->
                new ProviderLookupResult.Success(
                    List.of(
                        new com.incode.verification.domain.entity.Company(
                            "A", "A", LocalDate.parse("2020-01-01"), "x", true)));
    var service =
        new VerificationApplicationService(
            repository,
            new NoopCoordination(),
            provider,
            provider,
            Clock.fixed(now, ZoneOffset.UTC),
            Duration.ofMinutes(1));
    VerificationView view =
        ScopedValue.where(ExecutionContext.CURRENT, new ExecutionContext(UUID.randomUUID()))
            .call(() -> service.start(new StartVerificationCommand(UUID.randomUUID(), " acme ")));
    assertEquals(VerificationStatus.COMPLETED, view.status());
    assertThrows(
        UnsupportedOperationException.class, () -> view.otherResults().add(view.company()));
  }

  @Test
  void fallsBackOnlyForDomainFallbackFailures() {
    var repository = new MemoryRepository();
    var calls = new ArrayList<String>();
    var primary =
        (ProviderLookupPort)
            (q, c) -> {
              calls.add("primary");
              return new ProviderLookupResult.Failure(new ProviderFailure.Unavailable());
            };
    var fallback =
        (ProviderLookupPort)
            (q, c) -> {
              calls.add("fallback");
              return new ProviderLookupResult.Failure(new ProviderFailure.Timeout());
            };
    var service =
        new VerificationApplicationService(
            repository,
            new NoopCoordination(),
            primary,
            fallback,
            Clock.fixed(now, ZoneOffset.UTC),
            Duration.ofMinutes(1));
    assertThrows(
        ProviderSubmissionException.class,
        () ->
            ScopedValue.where(ExecutionContext.CURRENT, new ExecutionContext(UUID.randomUUID()))
                .call(
                    () -> service.start(new StartVerificationCommand(UUID.randomUUID(), "acme"))));
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
                new com.incode.verification.domain.valueobject.NormalizedQuery("ACME"),
                now,
                now.plusSeconds(60))
            .complete(
                new ProviderLookupResult.Success(
                    List.of(
                        new com.incode.verification.domain.entity.Company(
                            "A", "A", LocalDate.parse("2020-01-01"), "x", true)),
                    ProviderType.FREE),
                now);
    repository.values.put(id, verification);
    var providerCalls = new ArrayList<String>();
    var provider =
        (ProviderLookupPort)
            (q, c) -> {
              providerCalls.add(q.value());
              return new ProviderLookupResult.Success(List.of());
            };
    var service = service(repository, provider, provider);

    assertEquals(
        VerificationStatus.COMPLETED,
        service.start(new StartVerificationCommand(id, "acme")).status());
    assertThrows(
        com.incode.verification.application.service.VerificationConflictException.class,
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
            new com.incode.verification.domain.valueobject.NormalizedQuery("ACME"),
            now,
            now.plusSeconds(60)));
    var service =
        service(
            repository,
            (q, c) -> new ProviderLookupResult.Success(List.of()),
            (q, c) -> new ProviderLookupResult.Success(List.of()));
    var error =
        assertThrows(
            com.incode.verification.application.service.VerificationConflictException.class,
            () -> service.start(new StartVerificationCommand(id, "ACME")));
    assertEquals("VERIFICATION_IN_PROGRESS", error.code());
  }

  @Test
  void completesFromSharedCacheWithoutCallingProviders() {
    var repository = new MemoryRepository();
    var cached =
        new VerificationView(
            UUID.randomUUID(),
            "ACME",
            "ACME",
            now,
            now.plusSeconds(60),
            VerificationStatus.COMPLETED,
            new com.incode.verification.domain.entity.Company(
                "A", "A", LocalDate.parse("2020-01-01"), "x", true),
            List.of(),
            ProviderType.PREMIUM,
            null);
    var service =
        new VerificationApplicationService(
            repository,
            new CachedCoordination(cached),
            (q, c) -> {
              throw new AssertionError("provider must not be called");
            },
            (q, c) -> {
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
        new VerificationApplicationService(
            repository,
            new WaitingCoordination(),
            (q, c) -> {
              throw new AssertionError("provider must not be called");
            },
            (q, c) -> {
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
        new VerificationApplicationService(
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
            new com.incode.verification.domain.valueobject.NormalizedQuery("ACME"),
            now,
            now.plusSeconds(60)));
    var company =
        new com.incode.verification.domain.entity.Company(
            "A", "A", LocalDate.parse("2020-01-01"), "x", true);
    var cached =
        new VerificationView(
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
        new VerificationApplicationService(
            repository,
            new CachedCoordination(cached),
            (q, c) -> new ProviderLookupResult.Success(List.of(), ProviderType.FREE),
            (q, c) -> new ProviderLookupResult.Success(List.of(), ProviderType.FREE),
            Clock.fixed(now, ZoneOffset.UTC),
            Duration.ofMinutes(1));

    assertEquals(VerificationStatus.COMPLETED, service.get(id).status());
    assertInstanceOf(
        com.incode.verification.domain.type.VerificationState.Completed.class,
        repository.values.get(id).state());
  }

  @Test
  void sharedCachedFailureRemainsFailure() {
    var repository = new MemoryRepository();
    var failure = new ProviderFailure.Timeout();
    var cached =
        new VerificationView(
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
        new VerificationApplicationService(
            repository,
            new CachedCoordination(cached),
            (q, c) -> {
              throw new AssertionError("provider must not be called");
            },
            (q, c) -> {
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
                new com.incode.verification.domain.valueobject.NormalizedQuery("ACME"),
                now,
                now.plusSeconds(60))
            .fail(new ProviderFailure.Timeout(), now);
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
            new com.incode.verification.domain.valueobject.NormalizedQuery("ACME"),
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
            new com.incode.verification.domain.valueobject.NormalizedQuery(query),
            now,
            now.plusSeconds(60))
        .complete(new ProviderLookupResult.Success(List.of(company()), ProviderType.FREE), now);
  }

  private com.incode.verification.domain.entity.Company company() {
    return new com.incode.verification.domain.entity.Company(
        "A", "A", LocalDate.parse("2020-01-01"), "x", true);
  }

  private ProviderLookupPort providerMustNotRun() {
    return (q, c) -> {
      throw new AssertionError("provider must not be called");
    };
  }

  @Test
  void missingVerificationIsNotFound() {
    var repository = new MemoryRepository();
    var service =
        service(
            repository,
            (q, c) -> new ProviderLookupResult.Success(List.of()),
            (q, c) -> new ProviderLookupResult.Success(List.of()));
    assertInstanceOf(
        com.incode.verification.application.service.VerificationNotFoundException.class,
        assertThrows(
            com.incode.verification.application.service.VerificationNotFoundException.class,
            () -> service.get(UUID.randomUUID())));
  }

  private VerificationApplicationService service(
      MemoryRepository repository, ProviderLookupPort primary, ProviderLookupPort fallback) {
    return new VerificationApplicationService(
        repository,
        new NoopCoordination(),
        primary,
        fallback,
        Clock.fixed(now, ZoneOffset.UTC),
        Duration.ofMinutes(1));
  }

  private static final class MemoryRepository implements VerificationRepository {
    private final Map<UUID, Verification> values = new HashMap<>();
    private final Map<String, Verification> terminals = new HashMap<>();

    @Override
    public void insertInProgress(Verification v) {
      values.put(v.id(), v);
    }

    @Override
    public void update(Verification v) {
      values.put(v.id(), v);
    }

    @Override
    public Optional<Verification> findById(UUID id) {
      return Optional.ofNullable(values.get(id));
    }

    @Override
    public Optional<Verification> findTerminalByQuery(
        com.incode.verification.domain.valueobject.NormalizedQuery query) {
      return Optional.ofNullable(terminals.get(query.value()));
    }

    @Override
    public UUID claim(UUID id) {
      return values.containsKey(id) ? UUID.randomUUID() : null;
    }

    @Override
    public boolean updateTerminal(UUID id, UUID token, Verification v) {
      values.put(id, v);
      return true;
    }
  }

  private static class NoopCoordination implements CoordinationPort {
    @Override
    public Lease acquire(LookupKey key) {
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
    public Optional<VerificationView> cached(LookupKey key) {
      return Optional.empty();
    }

    @Override
    public void cache(LookupKey key, VerificationView view) {}
  }

  private static final class CachedCoordination extends NoopCoordination {
    private final VerificationView cached;

    CachedCoordination(VerificationView cached) {
      this.cached = cached;
    }

    @Override
    public Optional<VerificationView> cached(LookupKey key) {
      return Optional.of(cached);
    }
  }

  private static final class WaitingCoordination extends NoopCoordination {
    @Override
    public Lease acquire(LookupKey key) {
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
    public Lease acquire(LookupKey key) {
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
