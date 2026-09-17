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
  public VerificationResult start(StartVerificationCommand command) {
    var normalized = NormalizedQuery.normalize(command.query());
    var existing = verificationRepository.findById(command.verificationId());
    if (existing.isPresent()) {
      return existing(existing.orElseThrow(), normalized);
    }

    var now = Instant.now(clock);
    var verification =
        Verification.start(
            command.verificationId(), command.query(), normalized, now, now.plus(lifetime));
    if (!verificationRepository.insertInProgress(verification)) {
      return verificationRepository
          .findById(command.verificationId())
          .map(stored -> existing(stored, normalized))
          .orElseThrow();
    }

    return resolve(verification, normalized);
  }

  @Observed(name = "verification.get")
  public VerificationResult get(UUID verificationId) {
    return verificationRepository
        .findById(verificationId)
        .map(this::resolveForRead)
        .orElseThrow(
            () -> new VerificationNotFoundException("verification not found: " + verificationId));
  }

  private VerificationResult resolveForRead(Verification verification) {
    if (!(verification.state() instanceof VerificationState.InProgress)) {
      return VerificationResult.from(verification);
    }
    return verificationRecovery
        .recover(verification)
        .orElseGet(() -> VerificationResult.from(verification));
  }

  private VerificationResult resolve(Verification verification, NormalizedQuery query) {
    try (var lease = coordination.acquire(query)) {
      if (lease.degraded()) {
        throw new CoordinationUnavailableException("verification coordination is unavailable");
      }
      if (!lease.acquired()) {
        return sharedOrInProgress(verification);
      }

      var shared = verificationRepository.findByQuery(verification.query());
      if (shared.isPresent()) {
        return store.store(
            VerificationReconciliationMapper.fromShared(verification, shared.orElseThrow()), query);
      }

      var completed = verification.apply(providerService.resolve(verification.query()));
      return storeOrThrow(completed, query);
    }
  }

  private VerificationResult sharedOrInProgress(Verification verification) {
    var cached = verificationRecovery.cached(verification);
    if (cached.isPresent()) {
      return storeOrThrow(cached.orElseThrow());
    }
    return verificationRecovery
        .shared(verification)
        .orElseGet(() -> VerificationResult.from(verification));
  }

  private VerificationResult storeOrThrow(Verification verification, NormalizedQuery query) {
    return storeOrThrow(store.store(verification, query));
  }

  private VerificationResult storeOrThrow(VerificationResult result) {
    if (result.status() == VerificationStatus.FAILED) {
      throw new ProviderSubmissionException(
          Objects.requireNonNull(result.failure()), "provider resolution failed");
    }
    return result;
  }

  private VerificationResult existing(Verification stored, NormalizedQuery normalized) {
    if (!stored.query().equals(normalized)) {
      throw new VerificationConflictException(
          "VERIFICATION_ID_REUSE", "verificationId is already associated with another query");
    }
    if (stored.state() instanceof VerificationState.InProgress) {
      throw new VerificationConflictException(
          "VERIFICATION_IN_PROGRESS", "verification is already in progress");
    }
    return VerificationResult.from(stored);
  }
}
