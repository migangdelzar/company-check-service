package com.incode.verification.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;

import com.incode.verification.service.model.Verification;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ServiceModelArchitectureTest {
  @Test
  void serviceModelsRemainFrameworkFree() {
    assertFalse(
        Arrays.stream(Verification.class.getAnnotations())
            .anyMatch(
                annotation ->
                    annotation.annotationType().getName().startsWith("org.springframework.")));
  }
}
