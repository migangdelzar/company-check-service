package com.incode.verification.application.port.out;

import com.incode.verification.domain.valueobject.LookupKey;
import java.util.Optional;

public interface CoordinationPort {
  Lease acquire(LookupKey key);

  Optional<VerificationView> cached(LookupKey key);

  void cache(LookupKey key, VerificationView view);

  interface Lease extends AutoCloseable {
    boolean acquired();

    /** Redis is unavailable; callers must not invoke an external provider. */
    default boolean degraded() {
      return false;
    }

    @Override
    void close();
  }
}
