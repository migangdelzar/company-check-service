package com.incode.verification.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.incode.verification.service.model.NormalizedQuery;
import com.incode.verification.service.model.ProviderFailure;
import com.incode.verification.service.model.ProviderResult;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ReactivePortContractTest {
  @Test
  void providerClientEmitsReactiveResult() {
    ProviderClient client =
        ignored -> Mono.just(new ProviderResult.Failure(new ProviderFailure.Unavailable()));

    StepVerifier.create(client.lookup(new NormalizedQuery("acme")))
        .assertNext(
            result -> assertEquals(ProviderTypeResult.FAILURE, ProviderTypeResult.of(result)))
        .verifyComplete();
  }

  private enum ProviderTypeResult {
    FAILURE;

    static ProviderTypeResult of(ProviderResult result) {
      return result instanceof ProviderResult.Failure ? FAILURE : null;
    }
  }
}
