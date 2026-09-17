package com.incode.verification.adapter.out.ratelimit;

import com.incode.verification.domain.provider.ProviderType;

public interface ProviderRateLimiter {
  boolean tryAcquire(ProviderType provider);
}
