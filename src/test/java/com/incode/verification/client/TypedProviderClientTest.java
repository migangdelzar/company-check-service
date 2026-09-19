package com.incode.verification.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.incode.verification.service.model.NormalizedQuery;
import com.incode.verification.service.model.ProviderResult;
import com.incode.verification.service.model.ProviderType;
import java.util.List;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class TypedProviderClientTest {
  @Test
  void providerDecoratorsPreserveReactiveProviderIdentity() {
    var provider =
        new FreeProviderClient(
            query -> Mono.just(new ProviderResult.Success(List.of(), ProviderType.FREE)));

    var result = provider.lookup(new NormalizedQuery("ACME")).block();

    assertEquals(ProviderType.FREE, ((ProviderResult.Success) result).provider());
  }
}
