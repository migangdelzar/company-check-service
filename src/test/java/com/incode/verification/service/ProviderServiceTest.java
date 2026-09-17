package com.incode.verification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.incode.verification.client.ProviderClient;
import com.incode.verification.service.model.Company;
import com.incode.verification.service.model.NormalizedQuery;
import com.incode.verification.service.model.ProviderFailure;
import com.incode.verification.service.model.ProviderResult;
import com.incode.verification.service.model.ProviderType;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ProviderServiceTest {
  @Test
  void keepsFreeResultWhenItHasAnActiveMatch() {
    var premiumCalls = new AtomicInteger();
    ProviderClient free =
        query ->
            new ProviderResult.Success(
                List.of(
                    new Company("123", "Acme", LocalDate.parse("2020-01-01"), "1 Main St", true)),
                ProviderType.FREE);
    ProviderClient premium =
        query -> {
          premiumCalls.incrementAndGet();
          return new ProviderResult.Success(List.of(), ProviderType.PREMIUM);
        };

    var result = new ProviderService(free, premium).resolve(NormalizedQuery.normalize("123"));

    assertEquals(ProviderType.FREE, ((ProviderResult.Success) result).provider());
    assertEquals(0, premiumCalls.get());
  }

  @Test
  void doesNotFallBackForFreeClientErrors() {
    var premiumCalls = new AtomicInteger();
    ProviderClient free = query -> new ProviderResult.Failure(new ProviderFailure.ClientError(404));
    ProviderClient premium =
        query -> {
          premiumCalls.incrementAndGet();
          return new ProviderResult.Success(List.of(), ProviderType.PREMIUM);
        };

    var result = new ProviderService(free, premium).resolve(NormalizedQuery.normalize("123"));

    assertFalse(result instanceof ProviderResult.Success);
    assertEquals(0, premiumCalls.get());
  }

  @Test
  void fallsBackToPremiumWhenFreeReturnsNoCompanies() {
    var premiumCalls = new AtomicInteger();
    ProviderClient free = query -> new ProviderResult.Success(List.of(), ProviderType.FREE);
    ProviderClient premium =
        query -> {
          premiumCalls.incrementAndGet();
          return new ProviderResult.Success(
              List.of(new Company("123", "Acme", LocalDate.parse("2020-01-01"), "1 Main St", true)),
              ProviderType.PREMIUM);
        };

    var result = new ProviderService(free, premium).resolve(NormalizedQuery.normalize("123"));

    assertEquals(1, premiumCalls.get());
    assertEquals(ProviderType.PREMIUM, ((ProviderResult.Success) result).provider());
  }
}
