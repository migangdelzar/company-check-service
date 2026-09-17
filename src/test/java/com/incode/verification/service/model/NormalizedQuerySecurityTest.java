package com.incode.verification.service.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.incode.verification.exception.domain.InvalidQueryException;
import org.junit.jupiter.api.Test;

class NormalizedQuerySecurityTest {
  @Test
  void canonicalizesCompatibilityCharactersAndUnicodeWhitespace() {
    assertEquals("ACME", NormalizedQuery.normalize("\uFF21\uFF23\uFF2D\uFF25").value());
    assertEquals("ACME", NormalizedQuery.normalize("\u2003Acme\u2003").value());
  }

  @Test
  void rejectsControlCharactersInsteadOfPersistingOrForwardingThem() {
    assertThrows(
        InvalidQueryException.class, () -> NormalizedQuery.normalize("ACME\nX-Injected: true"));
    assertThrows(InvalidQueryException.class, () -> NormalizedQuery.normalize("ACME\u0000"));
    assertThrows(InvalidQueryException.class, () -> NormalizedQuery.normalize("ACME\u200B"));
  }
}
