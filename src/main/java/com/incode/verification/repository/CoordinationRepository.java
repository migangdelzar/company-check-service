package com.incode.verification.repository;

import com.incode.verification.service.model.NormalizedQuery;
import com.incode.verification.service.model.VerificationResult;
import reactor.core.publisher.Mono;

public interface CoordinationRepository {
  /**
   * Acquires coordination for a normalized query.
   *
   * @param query normalized query to coordinate
   * @return a lease that must be released by the caller
   */
  Mono<Lease> acquire(NormalizedQuery query);

  Mono<VerificationResult> get(NormalizedQuery query);

  Mono<VerificationResult> put(NormalizedQuery query, VerificationResult result);

  interface Lease {
    boolean acquired();

    /**
     * Indicates Redis is unavailable and callers must not invoke an external provider.
     *
     * @return true when coordination is degraded
     */
    default boolean degraded() {
      return false;
    }

    Mono<Void> release();
  }
}
