package com.incode.verification.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.incode.verification.repository.ratelimit.ProviderRateLimiter;
import com.incode.verification.service.model.NormalizedQuery;
import com.incode.verification.service.model.ProviderFailure;
import com.incode.verification.service.model.ProviderResult;
import com.incode.verification.service.model.ProviderType;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class DistributedProviderClientTest {
  @Test
  void rejectsProviderCallWhenTheSharedLimiterDeniesIt() {
    var invoked = new AtomicBoolean();
    ProviderClient delegate =
        query -> {
          invoked.set(true);
          return Mono.just(new ProviderResult.Success(java.util.List.of()));
        };
    ProviderRateLimiter limiter = provider -> Mono.just(false);

    var result =
        new DistributedFreeProviderClient(delegate, limiter)
            .lookup(new NormalizedQuery("ACME"))
            .block();

    assertInstanceOf(ProviderResult.Failure.class, result);
    assertInstanceOf(
        ProviderFailure.Unavailable.class, ((ProviderResult.Failure) result).failure());
    assertFalse(invoked.get());
  }

  @Test
  void delegatesProviderCallWhenTheSharedLimiterAllowsIt() {
    var invoked = new AtomicBoolean();
    ProviderClient delegate =
        query -> {
          invoked.set(true);
          return Mono.just(new ProviderResult.Success(java.util.List.of()));
        };
    ProviderRateLimiter limiter = provider -> Mono.just(provider == ProviderType.FREE);

    var result =
        new DistributedFreeProviderClient(delegate, limiter)
            .lookup(new NormalizedQuery("ACME"))
            .block();

    assertInstanceOf(ProviderResult.Success.class, result);
    assertTrue(invoked.get());
  }
}
