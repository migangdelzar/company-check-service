package com.incode.verification.application.service;

import com.incode.verification.application.context.ExecutionContext;
import com.incode.verification.application.port.in.GetVerificationUseCase;
import com.incode.verification.application.port.in.StartVerificationUseCase;
import com.incode.verification.application.port.out.CoordinationPort;
import com.incode.verification.application.port.out.ProviderLookupPort;
import com.incode.verification.application.port.out.VerificationAlreadyExistsException;
import com.incode.verification.application.port.out.VerificationLifecycle;
import com.incode.verification.application.port.out.VerificationRepository;
import com.incode.verification.application.port.out.VerificationView;
import com.incode.verification.domain.aggregate.Verification;
import com.incode.verification.domain.policy.FallbackPolicy;
import com.incode.verification.domain.type.ProviderLookupResult;
import com.incode.verification.domain.type.ProviderType;
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
    var verificationId = command.verificationId();
    var normalized = NormalizedQuery.normalize(command.query());
    var existing = repository.findById(verificationId);
    if (existing.isPresent()) {
      return existingResult(existing.orElseThrow(), normalized);
    }
    var key = new LookupKey(normalized);
    var now = Instant.now(clock);
    var verification =
        Verification.start(verificationId, command.query(), normalized, now, now.plus(lifetime));
    try {
      lifecycle.start(verification);
    } catch (VerificationAlreadyExistsException race) {
      return repository
          .findById(verificationId)
          .map(stored -> existingResult(stored, normalized))
          .orElseThrow(() -> race);
    }
    try (var lease = coordination.acquire(key)) {
      if (!lease.acquired()) {
        var cached = coordination.cached(key);
        if (cached.isPresent())
          return completeFromCached(verification, cached.orElseThrow(), key, true);
        var shared = repository.findTerminalByQuery(normalized);
        if (shared.isPresent()) return completeFromShared(verification, shared.orElseThrow(), key);
        return view(verification);
      }
      var cached = coordination.cached(key);
      if (cached.isPresent())
        return completeFromCached(verification, cached.orElseThrow(), key, true);
      var shared = repository.findTerminalByQuery(normalized);
      if (shared.isPresent()) return completeFromShared(verification, shared.orElseThrow(), key);
      var context = new ExecutionContext(UUID.randomUUID());
      var result = lookup(primaryProvider, normalized, context);
      if (FallbackPolicy.shouldFallback(result))
        result = lookup(fallbackProvider, normalized, context);
      var finalResult = result;
      var completed =
          result instanceof ProviderLookupResult.Success success
              ? verification.complete(success, Instant.now(clock))
              : verification.fail(
                  ((ProviderLookupResult.Failure) finalResult).failure(), Instant.now(clock));
      persistTerminal(completed);
      var resultView = view(completed);
      coordination.cache(key, resultView);
      if (completed.state() instanceof VerificationState.Failed failed)
        throw new ProviderSubmissionException(failed.failure(), "provider resolution failed");
      return resultView;
    }
  }

  @Override
  public VerificationView get(UUID verificationId) {
    return repository
        .findById(verificationId)
        .map(this::pollSharedTerminal)
        .orElseThrow(
            () -> new IllegalArgumentException("verification not found: " + verificationId));
  }

  private VerificationView pollSharedTerminal(Verification verification) {
    if (!(verification.state() instanceof VerificationState.InProgress)) return view(verification);
    var key = new LookupKey(verification.query());
    var cached = coordination.cached(key);
    if (cached.isPresent()) return completeFromCached(verification, cached.orElseThrow(), key, false);
    var shared = repository.findTerminalByQuery(verification.query());
    return shared
        .map(value -> completeFromShared(verification, value, key))
        .orElseGet(() -> view(verification));
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

  private VerificationView existingResult(Verification stored, NormalizedQuery normalized) {
    if (!stored.query().equals(normalized))
      throw new VerificationConflictException(
          "VERIFICATION_ID_REUSE", "verificationId is already associated with another query");
    if (stored.state() instanceof VerificationState.InProgress)
      throw new VerificationConflictException(
          "VERIFICATION_IN_PROGRESS", "verification is already in progress");
    return view(stored);
  }

  private ProviderLookupResult lookup(
      ProviderLookupPort provider, NormalizedQuery query, ExecutionContext context) {
    try {
      return ScopedValue.where(ExecutionContext.CURRENT, context)
          .call(() -> provider.lookup(query, context));
    } catch (RuntimeException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new IllegalStateException("provider lookup failed", exception);
    }
  }

  private VerificationView completeFromCached(
      Verification verification, VerificationView cached, LookupKey key, boolean raiseFailure) {
    if (cached.status() == VerificationStatus.FAILED) {
      var failed = verification.fail(cached.failure(), Instant.now(clock));
      persistTerminal(failed);
      coordination.cache(key, view(failed));
      if (raiseFailure)
        throw new ProviderSubmissionException(cached.failure(), "provider resolution failed");
      return view(failed);
    }
    var companies =
        java.util.stream.Stream.concat(
                java.util.stream.Stream.ofNullable(cached.company()),
                cached.otherResults().stream())
            .toList();
    var completed =
        verification.complete(
            new ProviderLookupResult.Success(
                companies, cached.provider() == null ? ProviderType.FREE : cached.provider()),
            Instant.now(clock));
    persistTerminal(completed);
    var result = view(completed);
    coordination.cache(key, result);
    return result;
  }

  private VerificationView completeFromShared(
      Verification verification, Verification shared, LookupKey key) {
    var hydrated =
        switch (shared.state()) {
          case VerificationState.InProgress ignored -> verification;
          case VerificationState.Completed completed ->
              verification.complete(
                  new ProviderLookupResult.Success(
                      java.util.stream.Stream.concat(
                              java.util.stream.Stream.ofNullable(completed.company()),
                              completed.otherResults().stream())
                          .toList(),
                      completed.provider()),
                  Instant.now(clock));
          case VerificationState.Failed failed ->
              verification.fail(failed.failure(), Instant.now(clock));
        };
    persistTerminal(hydrated);
    var result = view(hydrated);
    coordination.cache(key, result);
    return result;
  }

  private void persistTerminal(Verification verification) {
    var claimToken = repository.claim(verification.id());
    if (claimToken == null) throw new IllegalStateException("verification terminal claim lost");
    var updated = new boolean[1];
    lifecycle.transition(
        verification,
        r -> updated[0] = r.updateTerminal(verification.id(), claimToken, verification));
    if (!updated[0]) throw new IllegalStateException("verification terminal update lost ownership");
  }
}
