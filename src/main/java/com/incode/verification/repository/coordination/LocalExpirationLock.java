package com.incode.verification.repository.coordination;

import com.incode.verification.repository.ExpirationLock;

public final class LocalExpirationLock implements ExpirationLock {
  @Override
  public Lease tryAcquire() {
    return new Lease() {
      @Override
      public boolean acquired() {
        return true;
      }

      @Override
      public void close() {}
    };
  }
}
