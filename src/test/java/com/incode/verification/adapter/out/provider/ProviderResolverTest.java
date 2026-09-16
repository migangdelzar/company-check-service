package com.incode.verification.adapter.out.provider;

import static org.junit.jupiter.api.Assertions.assertSame;

import com.incode.verification.application.context.ExecutionContext;
import com.incode.verification.application.port.out.ProviderLookupPort;
import com.incode.verification.domain.type.ProviderFailure;
import com.incode.verification.domain.type.ProviderLookupResult;
import com.incode.verification.domain.type.ProviderType;
import com.incode.verification.domain.valueobject.NormalizedQuery;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProviderResolverTest {
  @Test
  void usesPremiumOnlyForApprovedFreeFailures() {
    var free = new Fixed(new ProviderLookupResult.Failure(new ProviderFailure.Unavailable()));
    var premium =
        new Fixed(new ProviderLookupResult.Success(java.util.List.of(), ProviderType.PREMIUM));
    var result =
        new ProviderResolver(free, premium)
            .lookup(new NormalizedQuery("acme"), new ExecutionContext(UUID.randomUUID()));
    assertSame(premium.result, result);
  }

  private static final class Fixed implements ProviderLookupPort {
    final ProviderLookupResult result;

    Fixed(ProviderLookupResult r) {
      result = r;
    }

    public ProviderLookupResult lookup(NormalizedQuery q, ExecutionContext c) {
      return result;
    }
  }
}
