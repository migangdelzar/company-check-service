package com.incode.verification.service;

import com.incode.verification.exception.CoordinationUnavailableException;
import com.incode.verification.exception.ProviderSubmissionException;
import com.incode.verification.exception.domain.VerificationConflictException;
import com.incode.verification.exception.domain.VerificationNotFoundException;
import com.incode.verification.mapper.VerificationReconciliationMapper;
import com.incode.verification.repository.CoordinationRepository;
import com.incode.verification.repository.VerificationRepository;
import com.incode.verification.service.model.NormalizedQuery;
import com.incode.verification.service.model.StartVerificationCommand;
import com.incode.verification.service.model.Verification;
import com.incode.verification.service.model.VerificationResult;
import com.incode.verification.service.model.VerificationState;
import com.incode.verification.service.model.VerificationStatus;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class VerificationService {
  private final VerificationRepository verificationRepository;
  private final CoordinationRepository coordination;
  private final ProviderService providerService;
  private final VerificationStoreService store;
  private final VerificationRecoveryService verificationRecovery;
  private final Clock clock;
  private final Duration lifetime;

  public VerificationService(
      VerificationRepository verificationRepository,
      CoordinationRepository coordination,
      ProviderService providerService,
      VerificationStoreService store,
      VerificationRecoveryService verificationRecovery,
      Clock clock,
      @Qualifier("verificationLifetime") Duration lifetime) {
    this.verificationRepository = verificationRepository;
    this.coordination = coordination;
    this.providerService = providerService;
    this.store = store;
    this.verificationRecovery = verificationRecovery;
    this.clock = clock;
    this.lifetime = lifetime;
  }

  @Observed(name = "verification.start")
  public Mono<VerificationResult> start(StartVerificationCommand command) {
    var normalized = NormalizedQuery.normalize(command.query());
    return verificationRepository
        .findById(command.verificationId())
        .flatMap(stored -> existing(stored, normalized))
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  var now = Instant.now(clock);
                  var verification =
                      Verification.start(
                          command.verificationId(),
                          command.query(),
                          normalized,
                          now,
                          now.plus(lifetime));
                  return verificationRepository
                      .insertInProgress(verification)
                      .flatMap(
                          inserted ->
                              inserted
                                  ? resolve(verification, normalized)
                                  : verificationRepository
                                      .findById(command.verificationId())
                                      .flatMap(stored -> existing(stored, normalized))
                                      .switchIfEmpty(
                                          Mono.error(
                                              () ->
                                                  new IllegalStateException(
                                                      "verification insert lost race without a stored row"))));
                }));
  }

  @Observed(name = "verification.get")
  public Mono<VerificationResult> get(UUID verificationId) {
    return verificationRepository
        .findById(verificationId)
        .flatMap(this::resolveForRead)
        .switchIfEmpty(
            Mono.error(
                () ->
                    new VerificationNotFoundException(
                        "verification not found: " + verificationId)));
  }

  private Mono<VerificationResult> resolveForRead(Verification verification) {
    if (!(verification.state() instanceof VerificationState.InProgress)) {
      return Mono.just(VerificationResult.from(verification));
    }
    return verificationRecovery
        .recover(verification)
        .defaultIfEmpty(VerificationResult.from(verification));
  }

  private Mono<VerificationResult> resolve(Verification verification, NormalizedQuery query) {
    return Mono.usingWhen(
        coordination.acquire(query),
        lease -> {
          if (lease.degraded()) {
            return Mono.error(
                new CoordinationUnavailableException("verification coordination is unavailable"));
          }
          if (!lease.acquired()) {
            return sharedOrInProgress(verification);
          }
          return verificationRepository
              .findByQuery(verification.query())
              .flatMap(
                  shared ->
                      store.store(
                          VerificationReconciliationMapper.fromShared(verification, shared), query))
              .switchIfEmpty(
                  providerService
                      .resolve(verification.query())
                      .map(verification::apply)
                      .flatMap(completed -> storeOrThrow(completed, query)));
        },
        CoordinationRepository.Lease::release);
  }

  private Mono<VerificationResult> sharedOrInProgress(Verification verification) {
    return verificationRecovery
        .cached(verification)
        .switchIfEmpty(verificationRecovery.shared(verification))
        .defaultIfEmpty(VerificationResult.from(verification));
  }

  private Mono<VerificationResult> storeOrThrow(Verification verification, NormalizedQuery query) {
    return storeOrThrow(store.store(verification, query));
  }

  private Mono<VerificationResult> storeOrThrow(Mono<VerificationResult> result) {
    return result.flatMap(
        value -> {
          if (value.status() == VerificationStatus.FAILED) {
            return Mono.error(
                new ProviderSubmissionException(
                    Objects.requireNonNull(value.failure()), "provider resolution failed"));
          }
          return Mono.just(value);
        });
  }

  private Mono<VerificationResult> existing(Verification stored, NormalizedQuery normalized) {
    if (!stored.query().equals(normalized)) {
      return Mono.error(
          new VerificationConflictException(
              "VERIFICATION_ID_REUSE", "verificationId is already associated with another query"));
    }
    if (stored.state() instanceof VerificationState.InProgress) {
      return Mono.error(
          new VerificationConflictException(
              "VERIFICATION_IN_PROGRESS", "verification is already in progress"));
    }
    return Mono.just(VerificationResult.from(stored));
  }
}
