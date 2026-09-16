package com.incode.verification.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class ModulithArchitectureTest {
  @Test
  void domainRemainsFrameworkFree() {
    assertFalse(
        java.util.Arrays.stream(
                com.incode.verification.domain.aggregate.Verification.class.getAnnotations())
            .anyMatch(
                annotation ->
                    annotation.annotationType().getName().startsWith("org.springframework.")));
  }
}
