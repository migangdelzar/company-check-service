package com.incode.verification.service;

import com.incode.verification.mapper.VerificationReconciliationMapper;
import com.incode.verification.repository.CoordinationRepository;
import com.incode.verification.repository.VerificationRepository;
import com.incode.verification.service.model.Verification;
import com.incode.verification.service.model.VerificationResult;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class VerificationRecoveryService {
  private final VerificationRepository repository;
  private final CoordinationRepository coordination;
  private final VerificationStoreService store;

  public VerificationRecoveryService(
      VerificationRepository repository,
      CoordinationRepository coordination,
      VerificationStoreService store) {
    this.repository = repository;
    this.coordination = coordination;
    this.store = store;
  }

  public Mono<VerificationResult> recover(Verification verification) {
    return cached(verification).switchIfEmpty(shared(verification));
  }

  public Mono<VerificationResult> cached(Verification verification) {
    var query = verification.query();
    return coordination
        .get(query)
        .filter(result -> result.status().isTerminal())
        .flatMap(
            result ->
                store.store(
                    VerificationReconciliationMapper.fromCached(verification, result), query));
  }

  public Mono<VerificationResult> shared(Verification verification) {
    var query = verification.query();
    return repository
        .findByQuery(query)
        .flatMap(
            shared ->
                store.store(
                    VerificationReconciliationMapper.fromShared(verification, shared), query));
  }
}
