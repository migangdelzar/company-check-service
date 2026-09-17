package com.incode.verification.application.service;

import com.incode.verification.application.port.out.CoordinationPort;
import com.incode.verification.application.port.out.VerificationRepository;
import com.incode.verification.application.result.VerificationResult;
import com.incode.verification.domain.query.NormalizedQuery;
import com.incode.verification.domain.verification.Verification;
import java.util.UUID;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class VerificationStoreService {
  private final VerificationRepository repository;
  private final CoordinationPort coordination;
  private final RetryTemplate databaseRetries;
  private final TransactionTemplate transaction;

  public VerificationStoreService(
      VerificationRepository repository,
      CoordinationPort coordination,
      RetryTemplate databaseRetries,
      TransactionTemplate transaction) {
    this.repository = repository;
    this.coordination = coordination;
    this.databaseRetries = databaseRetries;
    this.transaction = transaction;
  }

  public VerificationResult store(Verification verification, NormalizedQuery query) {
    return databaseRetries.invoke(
        () -> transaction.execute(status -> storeWithinTransaction(verification, query)));
  }

  private VerificationResult storeWithinTransaction(
      Verification verification, NormalizedQuery query) {
    var claimToken = repository.claim(verification.id());
    if (claimToken != null && repository.complete(verification.id(), claimToken, verification)) {
      var result = VerificationResult.from(verification);
      publishAfterCommit(query, result);
      return result;
    }
    return reload(verification.id(), query);
  }

  private VerificationResult reload(UUID id, NormalizedQuery query) {
    var current =
        repository
            .findById(id)
            .orElseThrow(() -> new VerificationNotFoundException("verification not found: " + id));
    var result = VerificationResult.from(current);
    publishAfterCommit(query, result);
    return result;
  }

  private void publishAfterCommit(NormalizedQuery query, VerificationResult result) {
    if (!result.status().isTerminal()) {
      return;
    }
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      coordination.put(query, result);
      return;
    }
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            coordination.put(query, result);
          }
        });
  }
}
