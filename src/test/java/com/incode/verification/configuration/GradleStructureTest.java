package com.incode.verification.configuration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class GradleStructureTest {
  private static final Path SERVICE_ROOT = Path.of(".");
  private static final Path BUILD_LOGIC_ROOT = SERVICE_ROOT.resolve("build-logic");

  @Test
  void includesFocusedBuildLogicAndServiceConvention() throws IOException {
    String settings = read(SERVICE_ROOT.resolve("settings.gradle.kts"));
    String rootBuild = read(SERVICE_ROOT.resolve("build.gradle.kts"));

    assertTrue(settings.contains("includeBuild(\"build-logic\")"));
    assertTrue(rootBuild.contains("id(\"com.incode.service-conventions\")"));
    assertTrue(Files.exists(BUILD_LOGIC_ROOT.resolve("settings.gradle.kts")));
    assertTrue(Files.exists(BUILD_LOGIC_ROOT.resolve("build.gradle.kts")));
    assertTrue(
        Files.exists(
            BUILD_LOGIC_ROOT.resolve("src/main/kotlin/com.incode.java-conventions.gradle.kts")));
    assertTrue(
        Files.exists(
            BUILD_LOGIC_ROOT.resolve(
                "src/main/kotlin/com.incode.spring-boot-conventions.gradle.kts")));
    assertTrue(
        Files.exists(
            BUILD_LOGIC_ROOT.resolve("src/main/kotlin/com.incode.service-conventions.gradle.kts")));
  }

  @Test
  void groupsFastChecksWithoutExternalVerificationTasks() throws IOException {
    String testing =
        read(BUILD_LOGIC_ROOT.resolve("src/main/kotlin/com.incode.testing-conventions.gradle.kts"));
    String fastCheck = testing.substring(testing.indexOf("fastCheck"));

    assertTrue(testing.contains("tasks.register(\"fastCheck\")"));
    assertTrue(fastCheck.contains("spotlessCheck"));
    assertTrue(fastCheck.contains("test"));
    assertFalse(fastCheck.contains("integrationTest"));
    assertFalse(fastCheck.contains("contractTest"));
    assertFalse(fastCheck.contains("e2eTest"));
    assertFalse(fastCheck.contains("openApiValidate"));
    assertFalse(fastCheck.contains("imageSmoke"));
    assertFalse(fastCheck.contains("bootBuildImage"));
  }

  @Test
  void keepsQualityOpenApiAndContainerConventionsSeparated() throws IOException {
    assertTrue(
        Files.exists(
            BUILD_LOGIC_ROOT.resolve("src/main/kotlin/com.incode.quality-conventions.gradle.kts")));
    assertTrue(
        Files.exists(
            BUILD_LOGIC_ROOT.resolve(
                "src/main/kotlin/com.incode.contract-conventions.gradle.kts")));
    assertTrue(
        Files.exists(
            BUILD_LOGIC_ROOT.resolve(
                "src/main/kotlin/com.incode.container-conventions.gradle.kts")));

    String quality =
        read(BUILD_LOGIC_ROOT.resolve("src/main/kotlin/com.incode.quality-conventions.gradle.kts"));
    String contracts =
        read(
            BUILD_LOGIC_ROOT.resolve("src/main/kotlin/com.incode.contract-conventions.gradle.kts"));
    String container =
        read(
            BUILD_LOGIC_ROOT.resolve(
                "src/main/kotlin/com.incode.container-conventions.gradle.kts"));

    assertTrue(quality.contains("qualityGate"));
    assertTrue(quality.contains("jacocoTestCoverageVerification"));
    assertTrue(contracts.contains("openApiValidate"));
    assertTrue(container.contains("imageSmoke"));
    assertTrue(container.contains("bootBuildImage"));
    assertTrue(container.contains("sha256"));
    assertFalse(Files.exists(SERVICE_ROOT.resolve("mise.toml")));
  }

  @Test
  void centralizesBuildLogicVersionsInTheVersionCatalog() throws IOException {
    String catalog = read(SERVICE_ROOT.resolve("gradle/libs.versions.toml"));
    String buildLogic = read(BUILD_LOGIC_ROOT.resolve("build.gradle.kts"));
    String quality =
        read(BUILD_LOGIC_ROOT.resolve("src/main/kotlin/com.incode.quality-conventions.gradle.kts"));
    String contracts =
        read(
            BUILD_LOGIC_ROOT.resolve("src/main/kotlin/com.incode.contract-conventions.gradle.kts"));
    String java =
        read(BUILD_LOGIC_ROOT.resolve("src/main/kotlin/com.incode.java-conventions.gradle.kts"));

    assertTrue(catalog.contains("java = \"25\""));
    assertTrue(catalog.contains("google-java-format = \"1.28.0\""));
    assertTrue(catalog.contains("redocly-cli = \"1.34.0\""));
    assertTrue(catalog.contains("spring-boot-gradle-plugin"));
    assertTrue(catalog.contains("spotless-gradle-plugin"));
    assertTrue(catalog.contains("error-prone-gradle-plugin"));
    assertTrue(buildLogic.contains("findLibrary(\"spring-boot-gradle-plugin\")"));
    assertTrue(buildLogic.contains("findLibrary(\"spotless-gradle-plugin\")"));
    assertTrue(buildLogic.contains("findLibrary(\"error-prone-gradle-plugin\")"));
    assertTrue(quality.contains("findVersion(\"google-java-format\")"));
    assertTrue(contracts.contains("findVersion(\"redocly-cli\")"));
    assertTrue(java.contains("findVersion(\"java\")"));
  }

  private String read(Path path) throws IOException {
    return Files.readString(path);
  }
}
