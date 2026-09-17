package com.incode.verification.application.port.out;

public interface ExpirationLock {
  Lease tryAcquire();

  interface Lease extends AutoCloseable {
    boolean acquired();

    @Override
    void close();
  }
}
