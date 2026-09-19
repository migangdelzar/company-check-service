package com.incode.verification.repository.ratelimit;

import com.incode.verification.service.model.ProviderType;
import reactor.core.publisher.Mono;

public interface ProviderRateLimiter {
  Mono<Boolean> tryAcquire(ProviderType provider);
}
