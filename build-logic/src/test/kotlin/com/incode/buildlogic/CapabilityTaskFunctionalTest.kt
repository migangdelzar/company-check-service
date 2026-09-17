package com.incode.buildlogic

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class CapabilityTaskFunctionalTest {
  @TempDir lateinit var projectDir: Path

  @Test
  fun capabilityAliasesAreExposed() {
    val result = runFixture(javaTestingQualityAndContractPlugins(), "tasks", "--all")

    assertTrue(result.output.contains("unitCheck"))
    assertTrue(result.output.contains("integrationCheck"))
    assertTrue(result.output.contains("contractCheck"))
    assertTrue(result.output.contains("e2eCheck"))
  }

  @Test
  fun fastCheckExcludesExternalCapabilities() {
    val result = runFixture(javaTestingQualityAndContractPlugins(), "fastCheck", "--dry-run")

    assertTrue(result.output.contains(":unitCheck"))
    assertFalse(result.output.contains(":integrationTest"))
    assertFalse(result.output.contains(":contractTest"))
    assertFalse(result.output.contains(":e2eTest"))
    assertFalse(result.output.contains(":openApiValidate"))
    assertFalse(result.output.contains(":imageSmoke"))
  }

  @Test
  fun qualityGateRetainsExternalVerification() {
    val result = runFixture(javaTestingQualityAndContractPlugins(), "qualityGate", "--dry-run")

    assertTrue(result.output.contains(":integrationTest"))
    assertTrue(result.output.contains(":contractTest"))
    assertTrue(result.output.contains(":e2eTest"))
    assertTrue(result.output.contains(":openApiValidate"))
  }

  @Test
  fun containerConventionExposesContainerCheck() {
    val result = runFixture(javaSpringBootAndContainerPlugins(), "tasks", "--all")

    assertTrue(result.output.contains("containerCheck"))
  }

  @Test
  fun containerCheckDryRunHandlesItsConfigurationCacheBoundary() {
    val result =
      runFixtureWithConfigurationCache(
        javaSpringBootAndContainerPlugins(),
        "containerCheck",
        "--dry-run",
        "-PpaketoBuilderImage=paketobuildpacks/builder-jammy-base@sha256:${"0".repeat(64)}",
        "-PpaketoRunImage=paketobuildpacks/run-jammy-base@sha256:${"0".repeat(64)}",
      )

    assertTrue(result.output.contains(":containerCheck"))
    assertTrue(result.output.contains(":imageSmoke"))
  }

  private fun javaTestingQualityAndContractPlugins(): String =
    """
    plugins {
      java
      id("com.incode.testing-conventions")
      id("com.incode.quality-conventions")
      id("com.incode.contract-conventions")
    }
    """.trimIndent()

  private fun javaSpringBootAndContainerPlugins(): String =
    """
    import org.gradle.jvm.toolchain.JavaLanguageVersion

    plugins {
      java
      id("org.springframework.boot") version "4.1.1"
      id("com.incode.container-conventions")
    }

    java {
      toolchain {
        languageVersion = JavaLanguageVersion.of(25)
      }
    }
    """.trimIndent()

  private fun runFixture(
    buildScript: String,
    vararg arguments: String,
  ): BuildResult {
    writeFixture(buildScript)

    return GradleRunner
      .create()
      .withProjectDir(projectDir.toFile())
      .withArguments(*arguments, "--stacktrace", "--no-configuration-cache")
      .build()
  }

  private fun runFixtureWithConfigurationCache(
    buildScript: String,
    vararg arguments: String,
  ): BuildResult {
    writeFixture(buildScript)
    Files.writeString(
      projectDir.resolve("gradle.properties"),
      "org.gradle.configuration-cache=true\norg.gradle.configuration-cache.problems=fail\n",
    )

    return GradleRunner
      .create()
      .withProjectDir(projectDir.toFile())
      .withArguments(*arguments, "--stacktrace")
      .build()
  }

  private fun writeFixture(buildScript: String) {
    Files.createDirectories(projectDir.resolve("gradle"))
    Files.createDirectories(projectDir.resolve("src/main/java/example"))
    Files.writeString(projectDir.resolve("settings.gradle.kts"), settingsScript())
    Files.writeString(projectDir.resolve("build.gradle.kts"), buildScript)
    Files.writeString(projectDir.resolve("gradle/libs.versions.toml"), versionCatalog())
    Files.writeString(
      projectDir.resolve("src/main/java/example/Fixture.java"),
      "package example; class Fixture {}",
    )
  }

  private fun settingsScript(): String {
    val buildLogicRoot = System.getProperty("buildLogicRoot").replace("\\", "\\\\")
    return """
      import org.gradle.api.initialization.resolve.RepositoriesMode

      pluginManagement {
        includeBuild("$buildLogicRoot")
        repositories {
          gradlePluginPortal()
          mavenCentral()
        }
      }
      dependencyResolutionManagement {
        repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
        repositories { mavenCentral() }
      }
      rootProject.name = "capability-fixture"
      """.trimIndent()
  }

  private fun versionCatalog(): String =
    """
    [versions]
    checkstyle = "10.21.2"
    google-java-format = "1.28.0"
    ktlint = "1.5.0"
    junit = "5.12.2"
    junit-platform = "1.12.2"
    testcontainers = "1.21.3"
    redocly-cli = "1.34.0"

    [libraries]
    junit-jupiter = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
    junit-platform-launcher = { module = "org.junit.platform:junit-platform-launcher", version.ref = "junit-platform" }
    testcontainers-junit-jupiter = { module = "org.testcontainers:junit-jupiter", version.ref = "testcontainers" }
    testcontainers = { module = "org.testcontainers:testcontainers", version.ref = "testcontainers" }
    testcontainers-postgresql = { module = "org.testcontainers:postgresql", version.ref = "testcontainers" }
    """.trimIndent()
}
