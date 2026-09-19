package com.incode.verification.repository.coordination;

import com.incode.verification.repository.ExpirationLock;
import reactor.core.publisher.Mono;

public final class LocalExpirationLock implements ExpirationLock {
  @Override
  public Mono<Lease> tryAcquire() {
    return Mono.just(
        new Lease() {
          @Override
          public boolean acquired() {
            return true;
          }

          @Override
          public Mono<Void> release() {
            return Mono.empty();
          }
        });
  }
}
