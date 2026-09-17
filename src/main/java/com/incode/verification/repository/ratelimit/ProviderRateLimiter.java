package com.incode.verification.repository.ratelimit;

import com.incode.verification.service.model.ProviderType;

public interface ProviderRateLimiter {
  boolean tryAcquire(ProviderType provider);
}
