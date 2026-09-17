package com.incode.verification.application.port.out;

import java.time.Duration;

public interface InboundRateLimiter {
  Decision tryAcquire();

  record Decision(Status status, Duration retryAfter) {
    public Decision {
      if (status == null) {
        throw new IllegalArgumentException("status must not be null");
      }
      if (retryAfter == null || retryAfter.isNegative()) {
        retryAfter = Duration.ZERO;
      }
    }

    public enum Status {
      ALLOWED,
      REJECTED,
      UNAVAILABLE
    }

    public boolean allowed() {
      return status == Status.ALLOWED;
    }
  }
}
