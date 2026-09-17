package com.incode.verification.adapter.out.coordination;

import com.incode.verification.application.port.out.ExpirationLock;

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
