package com.incode;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

class CompanyCheckApplicationTest {
  @Test
  void bootstrapIsTheSpringBootAndModulithRoot() {
    assertTrue(CompanyCheckApplication.class.isAnnotationPresent(SpringBootApplication.class));
  }
}
