package com.incode.verification.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;

import com.incode.CompanyCheckApplication;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModulithArchitectureTest {
  private static ApplicationModules modules;

  @BeforeAll
  static void discoverModules() {
    modules = ApplicationModules.of(CompanyCheckApplication.class);
  }

  @Test
  void modulithModuleDependenciesAreValid() {
    modules.verify();
  }

  @Test
  void domainRemainsFrameworkFree() {
    assertFalse(
        java.util.Arrays.stream(
                com.incode.verification.domain.verification.Verification.class.getAnnotations())
            .anyMatch(
                annotation ->
                    annotation.annotationType().getName().startsWith("org.springframework.")));
  }
}
