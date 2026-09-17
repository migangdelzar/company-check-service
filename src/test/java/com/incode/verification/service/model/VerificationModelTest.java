package com.incode.verification.service.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VerificationModelTest {
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
                new ProviderResult.Success(
                    List.of(
                        new Company("OFF", "off", LocalDate.parse("2020-01-01"), "x", false),
                        new Company("A", "A", LocalDate.parse("2020-01-01"), "x", true),
                        new Company("B", "B", LocalDate.parse("2020-01-01"), "y", true))));
    assertInstanceOf(VerificationState.Completed.class, v.state());
    var c = (VerificationState.Completed) v.state();
    assertEquals("A", c.company().name());
    assertEquals(
        List.of(new Company("B", "B", LocalDate.parse("2020-01-01"), "y", true)), c.otherResults());
  }

  @Test
  void rejectsDuplicateCompletion() {
    var v =
        verification()
            .complete(
                new ProviderResult.Success(
                    List.of(new Company("A", "A", LocalDate.parse("2020-01-01"), "x", true))));
    assertThrows(
        IllegalStateException.class, () -> v.complete(new ProviderResult.Success(List.of())));
  }

  @Test
  void createsUuidV7InternalIdentifiers() {
    var id = com.incode.verification.util.UuidV7.generate();
    assertEquals(7, id.version());
    assertEquals(2, id.variant());
  }

  @Test
  void createsMonotonicallyOrderedUuidV7Values() {
    var first = com.incode.verification.util.UuidV7.generate();
    var second = com.incode.verification.util.UuidV7.generate();

    assertTrue(first.compareTo(second) < 0);
  }
}
