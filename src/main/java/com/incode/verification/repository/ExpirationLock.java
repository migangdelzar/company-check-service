package com.incode.verification.repository;

import reactor.core.publisher.Mono;

public interface ExpirationLock {
  Mono<Lease> tryAcquire();

  interface Lease {
    boolean acquired();

    Mono<Void> release();
  }
}
