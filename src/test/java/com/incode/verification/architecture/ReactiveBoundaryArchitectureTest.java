package com.incode.verification.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ReactiveBoundaryArchitectureTest {
  private final JavaClasses productionClasses =
      new ClassFileImporter().importPath(Path.of("build/classes/java/main"));

  @Test
  void productionRequestPathHasNoBlockingTransportDependencies() {
    noClasses()
        .should()
        .dependOnClassesThat()
        .haveFullyQualifiedName("org.springframework.web.client.RestClient")
        .orShould()
        .dependOnClassesThat()
        .haveFullyQualifiedName("org.springframework.jdbc.core.simple.JdbcClient")
        .orShould()
        .dependOnClassesThat()
        .haveFullyQualifiedName("org.springframework.data.redis.core.StringRedisTemplate")
        .orShould()
        .dependOnClassesThat()
        .haveFullyQualifiedName("jakarta.servlet.Filter")
        .orShould()
        .dependOnClassesThat()
        .resideInAnyPackage("org.apache.hc..")
        .check(productionClasses);
  }

  @Test
  void productionSourcesDoNotBlockReactivePublishers() throws IOException {
    try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
      for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
        String source = Files.readString(file);
        assertFalse(source.contains(".block("), "blocking call found in " + file);
      }
    }
  }
}
