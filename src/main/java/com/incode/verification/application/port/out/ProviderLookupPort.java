package com.incode.verification.application.port.out;

import com.incode.verification.application.context.ExecutionContext;
import com.incode.verification.domain.type.ProviderLookupResult;
import com.incode.verification.domain.valueobject.NormalizedQuery;

/** Provider-neutral boundary; adapters translate vendor responses into domain results. */
public interface ProviderLookupPort {
  ProviderLookupResult lookup(NormalizedQuery query, ExecutionContext context);
}
