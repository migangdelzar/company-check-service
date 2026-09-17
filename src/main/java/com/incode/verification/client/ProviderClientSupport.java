package com.incode.verification.client;

import com.incode.verification.exception.ProviderContractException;
import com.incode.verification.exception.ProviderTransientException;
import com.incode.verification.service.model.NormalizedQuery;
import com.incode.verification.service.model.ProviderFailure;
import com.incode.verification.service.model.ProviderResult;

abstract class ProviderClientSupport implements ProviderClient {
  protected final ProviderClient delegate;

  ProviderClientSupport(ProviderClient delegate) {
    this.delegate = delegate;
  }

  public ProviderResult fallback(NormalizedQuery query, Throwable failure) {
    if (failure instanceof ProviderTransientException transientFailure) {
      return new ProviderResult.Failure(transientFailure.failure());
    }
    if (failure instanceof ProviderContractException) {
      return new ProviderResult.Failure(new ProviderFailure.Malformed());
    }
    return new ProviderResult.Failure(new ProviderFailure.Unavailable());
  }
}
