package com.incode.verification.adapter.out.provider;

import com.incode.verification.adapter.out.provider.ProviderContractException;
import com.incode.verification.adapter.out.provider.ProviderTransientException;
import com.incode.verification.application.port.out.ProviderLookupPort;
import com.incode.verification.domain.provider.ProviderFailure;
import com.incode.verification.domain.provider.ProviderResult;
import com.incode.verification.domain.query.NormalizedQuery;

abstract class ResilientProvider implements ProviderLookupPort {
  protected final ProviderLookupPort delegate;

  ResilientProvider(ProviderLookupPort delegate) {
    this.delegate = delegate;
  }

  public ProviderResult fallback(
      NormalizedQuery query, Throwable failure) {
    if (failure instanceof ProviderTransientException transientFailure) {
      return new ProviderResult.Failure(transientFailure.failure());
    }
    if (failure instanceof ProviderContractException) {
      return new ProviderResult.Failure(new ProviderFailure.Malformed());
    }
    return new ProviderResult.Failure(new ProviderFailure.Unavailable());
  }
}
