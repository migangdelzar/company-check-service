package com.incode.verification.adapter.out.provider;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.incode.verification.adapter.out.ratelimit.ProviderRateLimiter;
import com.incode.verification.application.port.out.ProviderLookupPort;
import com.incode.verification.domain.provider.ProviderFailure;
import com.incode.verification.domain.provider.ProviderResult;
import com.incode.verification.domain.provider.ProviderType;
import com.incode.verification.domain.query.NormalizedQuery;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class DistributedProviderTest {
  @Test
  void rejectsProviderCallWhenTheSharedLimiterDeniesIt() {
    var invoked = new AtomicBoolean();
    ProviderLookupPort delegate =
        query -> {
          invoked.set(true);
          return new ProviderResult.Success(java.util.List.of());
        };
    ProviderRateLimiter limiter = provider -> false;

    var result =
        new DistributedFreeProvider(delegate, limiter).lookup(new NormalizedQuery("ACME"));

    assertInstanceOf(ProviderResult.Failure.class, result);
    assertInstanceOf(
        ProviderFailure.Unavailable.class,
        ((ProviderResult.Failure) result).failure());
    assertFalse(invoked.get());
  }

  @Test
  void delegatesProviderCallWhenTheSharedLimiterAllowsIt() {
    var invoked = new AtomicBoolean();
    ProviderLookupPort delegate =
        query -> {
          invoked.set(true);
          return new ProviderResult.Success(java.util.List.of());
        };
    ProviderRateLimiter limiter = provider -> provider == ProviderType.FREE;

    var result =
        new DistributedFreeProvider(delegate, limiter).lookup(new NormalizedQuery("ACME"));

    assertInstanceOf(ProviderResult.Success.class, result);
    assertTrue(invoked.get());
  }
}
