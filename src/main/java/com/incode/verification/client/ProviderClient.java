package com.incode.verification.client;

import com.incode.verification.service.model.NormalizedQuery;
import com.incode.verification.service.model.ProviderResult;
import reactor.core.publisher.Mono;

/** Provider-neutral boundary; clients translate vendor responses into service results. */
public interface ProviderClient {
  Mono<ProviderResult> lookup(NormalizedQuery query);
}
