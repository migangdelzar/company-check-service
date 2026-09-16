import org.gradle.api.tasks.testing.Test
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.jvm.JvmTestSuite

plugins { id("jvm-test-suite") }

val libsCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
  systemProperty("junit.jupiter.execution.parallel.enabled", "false")
  group = if (name == "test") "verification" else "integration"
}

testing {
  suites {
    listOf("integrationTest", "contractTest", "e2eTest").forEach { suiteName ->
      register<JvmTestSuite>(suiteName) {
        useJUnitJupiter()
        sources { java.setSrcDirs(listOf("src/$suiteName/java")) }
        dependencies {
          implementation(project())
          if (suiteName == "integrationTest") {
            implementation(libsCatalog.findLibrary("testcontainers-junit-jupiter").get())
            implementation(libsCatalog.findLibrary("testcontainers-postgresql").get())
          }
        }
        targets.configureEach {
          testTask.configure {
            outputs.cacheIf { false }
            outputs.upToDateWhen { false }
          }
        }
      }
    }
  }
}

listOf("integrationTest", "contractTest", "e2eTest").forEach { suiteName ->
  configurations.named("${suiteName}Implementation") {
    extendsFrom(configurations.named("testImplementation").get())
  }
  configurations.named("${suiteName}RuntimeOnly") {
    extendsFrom(configurations.named("testRuntimeOnly").get())
  }
}

tasks.named("check") { dependsOn("integrationTest", "contractTest", "e2eTest") }

tasks.register("fastCheck") {
  group = "verification"
  description = "Runs unit tests and local quality checks without external-service validation."
  dependsOn(
    "spotlessCheck",
    "checkstyleMain",
    "checkstyleTest",
    "test",
    "jacocoTestReport",
    "jacocoTestCoverageVerification",
  )
}
