package com.incode.verification.application.port.out;

import com.incode.verification.domain.aggregate.Verification;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** PostgreSQL adapter port. Implementations must make save atomic with their transaction. */
public interface VerificationRepository {
  void insertInProgress(Verification verification);

  void update(Verification verification);

  Optional<Verification> findById(UUID id);

  default UUID claim(UUID id) {
    throw new UnsupportedOperationException("claiming is adapter-specific");
  }

  default boolean updateTerminal(UUID id, UUID claimToken, Verification verification) {
    throw new UnsupportedOperationException("terminal CAS is adapter-specific");
  }

  default int expireBatch(Instant now, int limit) {
    throw new UnsupportedOperationException("expiration is adapter-specific");
  }
}
