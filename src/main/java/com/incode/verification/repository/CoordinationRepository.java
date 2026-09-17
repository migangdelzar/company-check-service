package com.incode.verification.repository;

import com.incode.verification.service.model.NormalizedQuery;
import com.incode.verification.service.model.VerificationResult;
import java.util.Optional;

public interface CoordinationRepository {
  /**
   * Acquires coordination for a normalized query.
   *
   * @param query normalized query to coordinate
   * @return a lease that must be closed by the caller
   */
  Lease acquire(NormalizedQuery query);

  Optional<VerificationResult> get(NormalizedQuery query);

  VerificationResult put(NormalizedQuery query, VerificationResult result);

  interface Lease extends AutoCloseable {
    boolean acquired();

    /**
     * Indicates Redis is unavailable and callers must not invoke an external provider.
     *
     * @return true when coordination is degraded
     */
    default boolean degraded() {
      return false;
    }

    @Override
    void close();
  }
}
