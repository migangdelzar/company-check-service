package com.incode.verification.domain;

import static org.junit.jupiter.api.Assertions.*;

import com.incode.verification.domain.aggregate.Verification;
import com.incode.verification.domain.entity.Company;
import com.incode.verification.domain.type.*;
import com.incode.verification.domain.valueobject.NormalizedQuery;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class VerificationTest {
  private final Instant now = Instant.parse("2026-01-01T00:00:00Z");

  private Verification verification() {
    return Verification.start(
        UUID.randomUUID(), " acme ", NormalizedQuery.normalize(" acme "), now, now.plusSeconds(60));
  }

  @Test
  void normalizesRootLocaleAndValidatesBounds() {
    assertEquals("ACME", NormalizedQuery.normalize(" acme ").value());
    assertThrows(IllegalArgumentException.class, () -> NormalizedQuery.normalize(" "));
  }

  @Test
  void completesWithFirstActiveAndOtherResults() {
    var v =
        verification()
            .complete(
                new ProviderLookupResult.Success(
                    List.of(
                        new Company("off", "x", false),
                        new Company("A", "x", true),
                        new Company("B", "y", true))),
                now);
    assertInstanceOf(VerificationState.Completed.class, v.state());
    var c = (VerificationState.Completed) v.state();
    assertEquals("A", c.company().name());
    assertEquals(List.of(new Company("B", "y", true)), c.otherResults());
  }

  @Test
  void rejectsDuplicateCompletion() {
    var v =
        verification()
            .complete(new ProviderLookupResult.Success(List.of(new Company("A", "x", true))), now);
    assertThrows(
        IllegalStateException.class,
        () -> v.complete(new ProviderLookupResult.Success(List.of()), now));
  }

  @Test
  void mapsFallbackFailuresExhaustively() {
    assertTrue(
        com.incode.verification.domain.policy.FallbackPolicy.shouldFallback(
            new ProviderLookupResult.Failure(new ProviderFailure.Malformed())));
    assertFalse(
        com.incode.verification.domain.policy.FallbackPolicy.shouldFallback(
            new ProviderLookupResult.Failure(new ProviderFailure.Timeout())));
  }
}
