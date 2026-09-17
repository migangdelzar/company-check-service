package com.incode.verification.config;

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
  void includesFocusedBuildLogicAndDirectSpringBootPlugin() throws IOException {
    String settings = read(SERVICE_ROOT.resolve("settings.gradle.kts"));
    String rootBuild = read(SERVICE_ROOT.resolve("build.gradle.kts"));

    assertTrue(settings.contains("includeBuild(\"build-logic\")"));
    assertTrue(rootBuild.contains("java"));
    assertTrue(rootBuild.contains("group = \"com.incode.verification\""));
    assertTrue(rootBuild.contains("id(\"com.incode.testing-conventions\")"));
    assertTrue(rootBuild.contains("id(\"com.incode.quality-conventions\")"));
    assertTrue(rootBuild.contains("id(\"com.incode.contract-conventions\")"));
    assertTrue(rootBuild.contains("id(\"com.incode.container-conventions\")"));
    assertTrue(Files.exists(BUILD_LOGIC_ROOT.resolve("settings.gradle.kts")));
    assertTrue(Files.exists(BUILD_LOGIC_ROOT.resolve("build.gradle.kts")));
    assertTrue(rootBuild.contains("alias(libs.plugins.spring.boot)"));
    assertFalse(
        Files.exists(
            BUILD_LOGIC_ROOT.resolve(
                "src/main/kotlin/com.incode.spring-boot-conventions.gradle.kts")));
    assertFalse(
        Files.exists(
            BUILD_LOGIC_ROOT.resolve("src/main/kotlin/com.incode.service-conventions.gradle.kts")));
  }

  @Test
  void groupsFastChecksWithoutExternalVerificationTasks() throws IOException {
    String testing =
        read(BUILD_LOGIC_ROOT.resolve("src/main/kotlin/com.incode.testing-conventions.gradle.kts"));
    String fastCheck = testing.substring(testing.indexOf("fastCheck"));

    assertTrue(testing.contains("tasks.register(\"fastCheck\")"));
    assertTrue(testing.contains("integrationCheck"));
    assertTrue(testing.contains("e2eCheck"));
    assertTrue(fastCheck.contains("unitCheck"));
    assertFalse(fastCheck.contains("spotlessCheck"));
    assertFalse(fastCheck.contains("jacocoTestReport"));
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
    assertTrue(quality.contains("unitCheck"));
    assertTrue(quality.contains("jacocoTestCoverageVerification"));
    assertTrue(contracts.contains("openApiValidate"));
    assertTrue(contracts.contains("contractCheck"));
    assertTrue(container.contains("imageSmoke"));
    assertTrue(container.contains("containerCheck"));
    assertTrue(container.contains("bootBuildImage"));
    assertTrue(container.contains("BP_SPRING_AOT_ENABLED"));
    assertTrue(container.contains("sha256"));
    assertFalse(Files.exists(SERVICE_ROOT.resolve("mise.toml")));
  }

  @Test
  void centralizesBuildLogicVersionsInTheVersionCatalog() throws IOException {
    String catalog = read(SERVICE_ROOT.resolve("gradle/libs.versions.toml"));
    String rootBuild = read(SERVICE_ROOT.resolve("build.gradle.kts"));
    String buildLogic = read(BUILD_LOGIC_ROOT.resolve("build.gradle.kts"));
    String quality =
        read(BUILD_LOGIC_ROOT.resolve("src/main/kotlin/com.incode.quality-conventions.gradle.kts"));
    String contracts =
        read(
            BUILD_LOGIC_ROOT.resolve("src/main/kotlin/com.incode.contract-conventions.gradle.kts"));

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
    assertTrue(rootBuild.contains("libs.versions.java"));
  }

  @Test
  void configuresBuildAndConfigurationCachesExplicitly() throws IOException {
    String settings = read(SERVICE_ROOT.resolve("settings.gradle.kts"));
    String properties = read(SERVICE_ROOT.resolve("gradle.properties"));
    String example = read(SERVICE_ROOT.resolve("gradle.properties.example"));

    assertTrue(settings.contains("buildCache"));
    assertTrue(settings.contains(".gradle/build-cache"));
    assertTrue(settings.contains("remoteBuildCacheUrl"));
    assertTrue(properties.contains("org.gradle.configuration-cache.problems=fail"));
    assertTrue(example.contains("remoteBuildCacheUrl"));
    assertTrue(example.contains("localBuildCache"));
  }

  @Test
  void pinsGradleWrapperDistributionChecksum() throws IOException {
    String wrapper = read(SERVICE_ROOT.resolve("gradle/wrapper/gradle-wrapper.properties"));

    assertTrue(wrapper.contains("distributionSha256Sum="));
  }

  @Test
  void configuresStablePaketoCachesAndSingleFinalGate() throws IOException {
    String container =
        read(
            BUILD_LOGIC_ROOT.resolve(
                "src/main/kotlin/com.incode.container-conventions.gradle.kts"));
    String quality =
        read(BUILD_LOGIC_ROOT.resolve("src/main/kotlin/com.incode.quality-conventions.gradle.kts"));

    assertTrue(container.contains("paketoCacheVolumePrefix"));
    assertTrue(container.contains("buildCache"));
    assertTrue(container.contains("launchCache"));
    assertTrue(container.contains("buildWorkspace"));
    assertTrue(container.contains("paketoPullPolicy"));
    assertTrue(quality.contains("tasks.named(\"check\")"));
    assertTrue(quality.contains("verifyFinalGates"));
  }

  @Test
  void forwardsExplicitTestcontainersDockerHostToIntegrationTests() throws IOException {
    String testing =
        read(BUILD_LOGIC_ROOT.resolve("src/main/kotlin/com.incode.testing-conventions.gradle.kts"));

    assertTrue(testing.contains("testcontainersDockerHost"));
    assertTrue(testing.contains("environment(\"DOCKER_HOST\""));
    assertTrue(testing.contains("systemProperty(\"docker.host\""));
    assertTrue(testing.contains("testcontainersDockerSocketOverride"));
    assertTrue(testing.contains("TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE"));
  }

  @Test
  void configuresJavaSpotlessWithTwoSpaceGoogleFormatting() throws IOException {
    String quality =
        read(BUILD_LOGIC_ROOT.resolve("src/main/kotlin/com.incode.quality-conventions.gradle.kts"));

    assertTrue(quality.contains("target(\"src/**/*.java\")"));
    assertTrue(quality.contains("googleJavaFormat"));
    assertTrue(quality.contains("trimTrailingWhitespace()"));
    assertTrue(quality.contains("endWithNewline()"));
  }

  @Test
  void excludesGeneratedNativeImageSourcesFromCheckstyle() throws IOException {
    String quality =
        read(BUILD_LOGIC_ROOT.resolve("src/main/kotlin/com.incode.quality-conventions.gradle.kts"));

    assertTrue(quality.contains("task.name == \"checkstyleAot\""));
    assertTrue(quality.contains("task.name == \"checkstyleAotTest\""));
    assertTrue(quality.contains("enabled = false"));
  }

  private String read(Path path) throws IOException {
    return Files.readString(path);
  }
}
