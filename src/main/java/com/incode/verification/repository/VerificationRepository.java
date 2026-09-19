package com.incode.verification.repository;

import com.incode.verification.service.model.NormalizedQuery;
import com.incode.verification.service.model.Verification;
import java.time.Instant;
import java.util.UUID;
import reactor.core.publisher.Mono;

/** PostgreSQL repository boundary. Implementations must make save atomic with their transaction. */
public interface VerificationRepository {
  Mono<Boolean> insertInProgress(Verification verification);

  Mono<Verification> findById(UUID id);

  default Mono<Verification> findByQuery(NormalizedQuery query) {
    return Mono.empty();
  }

  default Mono<UUID> claim(UUID id) {
    return Mono.error(new UnsupportedOperationException("claiming is repository-specific"));
  }

  default Mono<Boolean> complete(UUID id, UUID claimToken, Verification verification) {
    return Mono.error(new UnsupportedOperationException("terminal CAS is repository-specific"));
  }

  default Mono<Integer> expireBatch(Instant now, int limit) {
    return Mono.error(new UnsupportedOperationException("expiration is repository-specific"));
  }
}
