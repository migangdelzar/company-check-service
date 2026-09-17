package com.incode.verification.repository;

public interface ExpirationLock {
  Lease tryAcquire();

  interface Lease extends AutoCloseable {
    boolean acquired();

    @Override
    void close();
  }
}
