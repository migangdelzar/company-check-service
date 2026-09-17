package com.incode.verification.application.service;

import com.incode.verification.application.port.out.ProviderLookupPort;
import com.incode.verification.domain.provider.FallbackPolicy;
import com.incode.verification.domain.provider.ProviderResult;
import com.incode.verification.domain.query.NormalizedQuery;
import io.micrometer.observation.annotation.Observed;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ProviderResolutionService {
  private final ProviderLookupPort primary;
  private final ProviderLookupPort fallback;

  public ProviderResolutionService(
      @Qualifier("freeProvider") ProviderLookupPort primary,
      @Qualifier("premiumProvider") ProviderLookupPort fallback) {
    this.primary = primary;
    this.fallback = fallback;
  }

  @Observed(name = "verification.provider.resolve")
  public ProviderResult resolve(NormalizedQuery query) {
    var result = lookup(primary, query);
    if (!FallbackPolicy.requiresFallback(result)) {
      return result;
    }
    return lookup(fallback, query);
  }

  private ProviderResult lookup(ProviderLookupPort provider, NormalizedQuery query) {
    return provider.lookup(query);
  }
}
