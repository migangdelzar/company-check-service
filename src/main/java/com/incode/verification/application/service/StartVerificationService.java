package com.incode.verification.application.service;

import com.incode.verification.application.port.in.StartVerificationCommand;
import com.incode.verification.application.port.in.StartVerificationUseCase;
import com.incode.verification.application.port.out.CoordinationPort;
import com.incode.verification.application.port.out.VerificationRepository;
import com.incode.verification.application.result.VerificationResult;
import com.incode.verification.domain.query.NormalizedQuery;
import com.incode.verification.domain.verification.Verification;
import com.incode.verification.domain.verification.VerificationState;
import com.incode.verification.domain.verification.VerificationStatus;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class StartVerificationService implements StartVerificationUseCase {
  private final VerificationRepository verificationRepository;
  private final CoordinationPort coordination;
  private final ProviderResolutionService providerLookup;
  private final VerificationStoreService store;
  private final VerificationRecoveryService verificationRecovery;
  private final Clock clock;
  private final Duration lifetime;

  public StartVerificationService(
      VerificationRepository verificationRepository,
      CoordinationPort coordination,
      ProviderResolutionService providerLookup,
      VerificationStoreService store,
      VerificationRecoveryService verificationRecovery,
      Clock clock,
      @Qualifier("verificationLifetime") Duration lifetime) {
    this.verificationRepository = verificationRepository;
    this.coordination = coordination;
    this.providerLookup = providerLookup;
    this.store = store;
    this.verificationRecovery = verificationRecovery;
    this.clock = clock;
    this.lifetime = lifetime;
  }

  @Override
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
            VerificationReconciliation.fromShared(verification, shared.orElseThrow()), query);
      }

      var completed = verification.apply(providerLookup.resolve(verification.query()));
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
