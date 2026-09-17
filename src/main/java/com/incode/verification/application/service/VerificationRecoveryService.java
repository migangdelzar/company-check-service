package com.incode.verification.application.service;

import com.incode.verification.application.port.out.CoordinationPort;
import com.incode.verification.application.port.out.VerificationRepository;
import com.incode.verification.application.result.VerificationResult;
import com.incode.verification.domain.verification.Verification;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class VerificationRecoveryService {
  private final VerificationRepository repository;
  private final CoordinationPort coordination;
  private final VerificationStoreService store;

  public VerificationRecoveryService(
      VerificationRepository repository,
      CoordinationPort coordination,
      VerificationStoreService store) {
    this.repository = repository;
    this.coordination = coordination;
    this.store = store;
  }

  public Optional<VerificationResult> recover(Verification verification) {
    return cached(verification).or(() -> shared(verification));
  }

  public Optional<VerificationResult> cached(Verification verification) {
    var query = verification.query();
    return coordination
        .get(query)
        .filter(result -> result.status().isTerminal())
        .map(
            result ->
                store.store(VerificationReconciliation.fromCached(verification, result), query));
  }

  public Optional<VerificationResult> shared(Verification verification) {
    var query = verification.query();
    return repository
        .findByQuery(query)
        .map(
            shared ->
                store.store(VerificationReconciliation.fromShared(verification, shared), query));
  }
}
