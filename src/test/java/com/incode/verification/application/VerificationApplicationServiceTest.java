package com.incode.verification.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.incode.verification.application.context.ExecutionContext;
import com.incode.verification.application.port.in.StartVerificationUseCase.StartVerificationCommand;
import com.incode.verification.application.port.out.CoordinationPort;
import com.incode.verification.application.port.out.ProviderLookupPort;
import com.incode.verification.application.port.out.VerificationLifecycle;
import com.incode.verification.application.port.out.VerificationRepository;
import com.incode.verification.application.port.out.VerificationView;
import com.incode.verification.application.service.VerificationApplicationService;
import com.incode.verification.domain.aggregate.Verification;
import com.incode.verification.domain.type.ProviderFailure;
import com.incode.verification.domain.type.ProviderLookupResult;
import com.incode.verification.domain.type.VerificationStatus;
import com.incode.verification.domain.valueobject.LookupKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
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
    var lifecycle = new RecordingLifecycle(repository);
    var provider =
        (ProviderLookupPort)
            (q, c) ->
                new ProviderLookupResult.Success(
                    List.of(new com.incode.verification.domain.entity.Company("A", "x", true)));
    var service =
        new VerificationApplicationService(
            repository,
            lifecycle,
            new NoopCoordination(),
            provider,
            provider,
            Clock.fixed(now, ZoneOffset.UTC),
            Duration.ofMinutes(1));
    VerificationView view =
        ScopedValue.where(ExecutionContext.CURRENT, new ExecutionContext(UUID.randomUUID()))
            .call(() -> service.start(new StartVerificationCommand(" acme ")));
    assertEquals(VerificationStatus.COMPLETED, view.status());
    assertThrows(
        UnsupportedOperationException.class, () -> view.otherResults().add(view.company()));
    assertEquals(List.of("start", "transition"), lifecycle.calls);
  }

  @Test
  void fallsBackOnlyForDomainFallbackFailures() {
    var repository = new MemoryRepository();
    var lifecycle = new RecordingLifecycle(repository);
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
            lifecycle,
            new NoopCoordination(),
            primary,
            fallback,
            Clock.fixed(now, ZoneOffset.UTC),
            Duration.ofMinutes(1));
    var view =
        ScopedValue.where(ExecutionContext.CURRENT, new ExecutionContext(UUID.randomUUID()))
            .call(() -> service.start(new StartVerificationCommand("acme")));
    assertEquals(VerificationStatus.FAILED, view.status());
    assertEquals(List.of("primary", "fallback"), calls);
  }

  private static final class MemoryRepository implements VerificationRepository {
    private final Map<UUID, Verification> values = new HashMap<>();

    public void insertInProgress(Verification v) {
      values.put(v.id(), v);
    }

    public void update(Verification v) {
      values.put(v.id(), v);
    }

    public Optional<Verification> findById(UUID id) {
      return Optional.ofNullable(values.get(id));
    }
  }

  private static final class RecordingLifecycle implements VerificationLifecycle {
    private final VerificationRepository repository;
    private final List<String> calls = new ArrayList<>();

    RecordingLifecycle(VerificationRepository repository) {
      this.repository = repository;
    }

    public void start(Verification v) {
      calls.add("start");
      repository.insertInProgress(v);
    }

    public void transition(
        Verification v, java.util.function.Consumer<VerificationRepository> write) {
      calls.add("transition");
      write.accept(repository);
    }
  }

  private static final class NoopCoordination implements CoordinationPort {
    public Lease acquire(LookupKey key) {
      return new Lease() {
        public boolean acquired() {
          return true;
        }

        public void close() {}
      };
    }

    public Optional<VerificationView> cached(LookupKey key) {
      return Optional.empty();
    }

    public void cache(LookupKey key, VerificationView view) {}
  }
}
