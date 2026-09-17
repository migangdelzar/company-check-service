import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.api.tasks.testing.Test

plugins {
  id("java-test-fixtures")
  id("jvm-test-suite")
}

val libsCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
val testSuiteNames = listOf("integrationTest", "contractTest", "e2eTest")
val testcontainersDockerHost =
  providers
    .gradleProperty("testcontainersDockerHost")
    .orElse(providers.environmentVariable("DOCKER_HOST"))
val testcontainersDockerSocketOverride =
  providers
    .gradleProperty("testcontainersDockerSocketOverride")
    .orElse(providers.environmentVariable("TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE"))
val maxTestForks =
  providers
    .gradleProperty("test.maxParallelForks")
    .map { value ->
      value.toIntOrNull()?.also { require(it > 0) }
        ?: error("test.maxParallelForks must be a positive integer")
    }.orElse(1)
val unitTest = tasks.named<Test>("test")

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
  systemProperty("junit.jupiter.execution.parallel.enabled", "false")
  testcontainersDockerHost.orNull?.let {
    environment("DOCKER_HOST", it)
    systemProperty("docker.host", it)
  }
  testcontainersDockerSocketOverride.orNull?.let {
    environment("TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE", it)
    systemProperty("docker.socket.override", it)
  }
  maxParallelForks = maxTestForks.get()
  group = if (name == "test") "verification" else "integration"
}

testing {
  suites {
    testSuiteNames.forEach { suiteName ->
      register<JvmTestSuite>(suiteName) {
        useJUnitJupiter()
        sources { java.setSrcDirs(listOf("src/$suiteName/java")) }
        dependencies {
          implementation(project())
          if (suiteName == "integrationTest") {
            implementation(libsCatalog.findLibrary("testcontainers-junit-jupiter").get())
            implementation(libsCatalog.findLibrary("testcontainers").get())
            implementation(libsCatalog.findLibrary("testcontainers-postgresql").get())
          }
        }
        targets.configureEach {
          testTask.configure {
            shouldRunAfter(unitTest)
            outputs.cacheIf { false }
            outputs.upToDateWhen { false }
          }
        }
      }
    }
  }
}

testSuiteNames.forEach { suiteName ->
  configurations.named("${suiteName}Implementation") {
    extendsFrom(configurations.named("testImplementation").get())
  }
  configurations.named("${suiteName}RuntimeOnly") {
    extendsFrom(configurations.named("testRuntimeOnly").get())
  }
}

tasks.named("check") { dependsOn("integrationTest", "contractTest", "e2eTest") }

tasks.register("integrationCheck") {
  group = "verification"
  description = "Runs integration tests against external service containers."
  dependsOn("integrationTest")
}

tasks.register("e2eCheck") {
  group = "verification"
  description = "Runs end-to-end verification tests."
  dependsOn("e2eTest")
}

tasks.register("fastCheck") {
  group = "verification"
  description = "Runs unit tests and local quality checks without external-service validation."
  dependsOn("unitCheck")
}
