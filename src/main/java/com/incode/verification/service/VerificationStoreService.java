package com.incode.verification.service;

import com.incode.verification.config.persistence.DatabaseRetryProperties;
import com.incode.verification.exception.domain.VerificationNotFoundException;
import com.incode.verification.repository.CoordinationRepository;
import com.incode.verification.repository.VerificationRepository;
import com.incode.verification.service.model.NormalizedQuery;
import com.incode.verification.service.model.Verification;
import com.incode.verification.service.model.VerificationResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Service
public class VerificationStoreService {
  private final VerificationRepository repository;
  private final CoordinationRepository coordination;
  private final TransactionalOperator transaction;
  private final Retry databaseRetries;

  public VerificationStoreService(
      VerificationRepository repository,
      CoordinationRepository coordination,
      TransactionalOperator transaction,
      DatabaseRetryProperties retryProperties) {
    this.repository = repository;
    this.coordination = coordination;
    this.databaseRetries =
        Retry.backoff(retryProperties.maxAttempts() - 1L, retryProperties.delay())
            .maxBackoff(retryProperties.maxDelay())
            .jitter(0d);
    this.transaction = transaction;
  }

  public Mono<VerificationResult> store(Verification verification, NormalizedQuery query) {
    return transaction
        .transactional(storeWithinTransaction(verification))
        .retryWhen(databaseRetries)
        .flatMap(
            result ->
                result.status().isTerminal() ? coordination.put(query, result) : Mono.just(result));
  }

  private Mono<VerificationResult> storeWithinTransaction(Verification verification) {
    return repository
        .claim(verification.id())
        .flatMap(
            claimToken ->
                repository
                    .complete(verification.id(), claimToken, verification)
                    .flatMap(
                        completed ->
                            completed
                                ? Mono.just(VerificationResult.from(verification))
                                : reload(verification.id())))
        .switchIfEmpty(Mono.defer(() -> reload(verification.id())));
  }

  private Mono<VerificationResult> reload(java.util.UUID id) {
    return repository
        .findById(id)
        .map(VerificationResult::from)
        .switchIfEmpty(
            Mono.error(() -> new VerificationNotFoundException("verification not found: " + id)));
  }
}
