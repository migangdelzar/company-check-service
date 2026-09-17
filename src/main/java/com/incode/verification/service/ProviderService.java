package com.incode.verification.service;

import com.incode.verification.client.ProviderClient;
import com.incode.verification.service.model.NormalizedQuery;
import com.incode.verification.service.model.ProviderFailure;
import com.incode.verification.service.model.ProviderResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ProviderService {
  private final ProviderClient free;
  private final ProviderClient premium;

  public ProviderService(
      @Qualifier("freeProvider") ProviderClient free,
      @Qualifier("premiumProvider") ProviderClient premium) {
    this.free = free;
    this.premium = premium;
  }

  @Observed(name = "verification.provider.resolve")
  public ProviderResult resolve(NormalizedQuery query) {
    var result = free.lookup(query);
    if (!requiresFallback(result)) {
      return result;
    }
    return premium.lookup(query);
  }

  private boolean requiresFallback(ProviderResult result) {
    return switch (result) {
      case ProviderResult.Success success -> success.companies().isEmpty();
      case ProviderResult.Failure failure ->
          switch (failure.failure()) {
            case ProviderFailure.Unavailable ignored -> true;
            case ProviderFailure.Malformed ignored -> true;
            case ProviderFailure.ClientError ignored -> false;
            case ProviderFailure.Timeout ignored -> true;
          };
    };
  }
}
