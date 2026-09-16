package com.incode.verification.adapter.out.provider;

import com.incode.verification.application.context.ExecutionContext;
import com.incode.verification.application.port.out.ProviderLookupPort;
import com.incode.verification.domain.policy.FallbackPolicy;
import com.incode.verification.domain.type.ProviderLookupResult;
import com.incode.verification.domain.valueobject.NormalizedQuery;

public final class ProviderResolver implements ProviderLookupPort {
    private final ProviderLookupPort free; private final ProviderLookupPort premium;
    public ProviderResolver(ProviderLookupPort free, ProviderLookupPort premium) { this.free = free; this.premium = premium; }
    @Override public ProviderLookupResult lookup(NormalizedQuery query, ExecutionContext context) {
        var result = free.lookup(query, context);
        return FallbackPolicy.shouldFallback(result) ? premium.lookup(query, context) : result;
    }
}
