package com.incode.verification.application.port.out;

import com.incode.verification.domain.query.NormalizedQuery;
import com.incode.verification.domain.verification.Verification;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** PostgreSQL adapter port. Implementations must make save atomic with their transaction. */
public interface VerificationRepository {
  boolean insertInProgress(Verification verification);

  Optional<Verification> findById(UUID id);

  default Optional<Verification> findByQuery(NormalizedQuery query) {
    return Optional.empty();
  }

  default @Nullable UUID claim(UUID id) {
    throw new UnsupportedOperationException("claiming is adapter-specific");
  }

  default boolean complete(UUID id, UUID claimToken, Verification verification) {
    throw new UnsupportedOperationException("terminal CAS is adapter-specific");
  }

  default int expireBatch(Instant now, int limit) {
    throw new UnsupportedOperationException("expiration is adapter-specific");
  }
}
