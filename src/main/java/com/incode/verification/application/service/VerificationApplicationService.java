package com.incode.verification.application.service;

import com.incode.verification.application.context.ExecutionContext;
import com.incode.verification.application.port.in.GetVerificationUseCase;
import com.incode.verification.application.port.in.StartVerificationUseCase;
import com.incode.verification.application.port.out.CoordinationPort;
import com.incode.verification.application.port.out.ProviderLookupPort;
import com.incode.verification.application.port.out.VerificationLifecycle;
import com.incode.verification.application.port.out.VerificationRepository;
import com.incode.verification.application.port.out.VerificationView;
import com.incode.verification.domain.aggregate.Verification;
import com.incode.verification.domain.policy.FallbackPolicy;
import com.incode.verification.domain.type.ProviderLookupResult;
import com.incode.verification.domain.type.VerificationState;
import com.incode.verification.domain.type.VerificationStatus;
import com.incode.verification.domain.valueobject.LookupKey;
import com.incode.verification.domain.valueobject.NormalizedQuery;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public final class VerificationApplicationService
    implements StartVerificationUseCase, GetVerificationUseCase {
  private final VerificationRepository repository;
  private final VerificationLifecycle lifecycle;
  private final CoordinationPort coordination;
  private final ProviderLookupPort primaryProvider;
  private final ProviderLookupPort fallbackProvider;
  private final Clock clock;
  private final Duration lifetime;

  public VerificationApplicationService(
      VerificationRepository repository,
      VerificationLifecycle lifecycle,
      CoordinationPort coordination,
      ProviderLookupPort primaryProvider,
      ProviderLookupPort fallbackProvider,
      Clock clock,
      Duration lifetime) {
    this.repository = repository;
    this.lifecycle = lifecycle;
    this.coordination = coordination;
    this.primaryProvider = primaryProvider;
    this.fallbackProvider = fallbackProvider;
    this.clock = clock;
    this.lifetime = lifetime;
  }

  @Override
  public VerificationView start(StartVerificationCommand command) {
    var normalized = NormalizedQuery.normalize(command.query());
    var key = new LookupKey(normalized);
    var cached = coordination.cached(key);
    if (cached.isPresent()) return cached.orElseThrow();
    var now = Instant.now(clock);
    var verification =
        Verification.start(UUID.randomUUID(), command.query(), normalized, now, now.plus(lifetime));
    lifecycle.start(verification);
    try (var lease = coordination.acquire(key)) {
      if (!lease.acquired())
        return view(repository.findById(verification.id()).orElse(verification));
      var result = primaryProvider.lookup(normalized, ExecutionContext.current());
      if (FallbackPolicy.shouldFallback(result))
        result = fallbackProvider.lookup(normalized, ExecutionContext.current());
      var finalResult = result;
      var completed =
          result instanceof ProviderLookupResult.Success success
              ? verification.complete(success, Instant.now(clock))
              : verification.fail(
                  ((ProviderLookupResult.Failure) finalResult).failure(), Instant.now(clock));
      lifecycle.transition(completed, r -> r.update(completed));
      var resultView = view(completed);
      coordination.cache(key, resultView);
      return resultView;
    }
  }

  @Override
  public VerificationView get(UUID verificationId) {
    return repository
        .findById(verificationId)
        .map(this::view)
        .orElseThrow(
            () -> new IllegalArgumentException("verification not found: " + verificationId));
  }

  private VerificationView view(Verification v) {
    return switch (v.state()) {
      case VerificationState.InProgress ignored ->
          new VerificationView(
              v.id(),
              v.rawQuery(),
              v.query().value(),
              v.startedAt(),
              v.expiresAt(),
              VerificationStatus.IN_PROGRESS,
              null,
              null,
              null,
              null);
      case VerificationState.Completed s ->
          new VerificationView(
              v.id(),
              v.rawQuery(),
              v.query().value(),
              v.startedAt(),
              v.expiresAt(),
              VerificationStatus.COMPLETED,
              s.company(),
              s.otherResults(),
              s.provider(),
              null);
      case VerificationState.Failed s ->
          new VerificationView(
              v.id(),
              v.rawQuery(),
              v.query().value(),
              v.startedAt(),
              v.expiresAt(),
              VerificationStatus.FAILED,
              null,
              null,
              null,
              s.failure());
    };
  }
}
