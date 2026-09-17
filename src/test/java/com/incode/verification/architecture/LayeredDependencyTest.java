package com.incode.verification.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class LayeredDependencyTest {
  private final JavaClasses verificationClasses =
      new ClassFileImporter().importPath(Path.of("build/classes/java/main"));

  @Test
  void controllersDoNotDependOnInfrastructureLayers() {
    noClasses()
        .that()
        .resideInAnyPackage("..verification.controller..")
        .and()
        .haveSimpleNameEndingWith("Controller")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "..verification.repository..", "..verification.client..", "..verification.config..")
        .check(verificationClasses);
  }

  @Test
  void servicesDoNotDependOnTransportOrInfrastructureDetails() {
    noClasses()
        .that()
        .resideInAnyPackage("..verification.service..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "..verification.controller..",
            "..verification.client.dto..",
            "..verification.repository.entity..",
            "org.springframework.web..",
            "org.springframework.jdbc..",
            "org.springframework.data.redis..",
            "org.apache.hc..")
        .check(verificationClasses);
  }

  @Test
  void clientsAndRepositoriesDoNotDependOnControllers() {
    noClasses()
        .that()
        .resideInAnyPackage("..verification.client..", "..verification.repository..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..verification.controller..")
        .check(verificationClasses);
  }

  @Test
  void serviceModelsRemainFrameworkFree() {
    noClasses()
        .that()
        .resideInAnyPackage("..verification.service.model..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "org.springframework..",
            "com.fasterxml.jackson..",
            "org.apache.hc..",
            "java.sql..",
            "javax.sql..")
        .check(verificationClasses);
  }
}
