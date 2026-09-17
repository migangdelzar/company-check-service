package com.incode.verification.application.port.out;

import com.incode.verification.domain.provider.ProviderResult;
import com.incode.verification.domain.query.NormalizedQuery;

/** Provider-neutral boundary; adapters translate vendor responses into domain results. */
public interface ProviderLookupPort {
  ProviderResult lookup(NormalizedQuery query);
}
