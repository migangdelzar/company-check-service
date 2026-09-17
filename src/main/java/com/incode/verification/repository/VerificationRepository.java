package com.incode.verification.repository;

import com.incode.verification.service.model.NormalizedQuery;
import com.incode.verification.service.model.Verification;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** PostgreSQL repository boundary. Implementations must make save atomic with their transaction. */
public interface VerificationRepository {
  boolean insertInProgress(Verification verification);

  Optional<Verification> findById(UUID id);

  default Optional<Verification> findByQuery(NormalizedQuery query) {
    return Optional.empty();
  }

  default @Nullable UUID claim(UUID id) {
    throw new UnsupportedOperationException("claiming is repository-specific");
  }

  default boolean complete(UUID id, UUID claimToken, Verification verification) {
    throw new UnsupportedOperationException("terminal CAS is repository-specific");
  }

  default int expireBatch(Instant now, int limit) {
    throw new UnsupportedOperationException("expiration is repository-specific");
  }
}
