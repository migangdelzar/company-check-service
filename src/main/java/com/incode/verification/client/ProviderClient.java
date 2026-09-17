package com.incode.verification.client;

import com.incode.verification.service.model.NormalizedQuery;
import com.incode.verification.service.model.ProviderResult;

/** Provider-neutral boundary; clients translate vendor responses into service results. */
public interface ProviderClient {
  ProviderResult lookup(NormalizedQuery query);
}
