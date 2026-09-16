import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import javax.inject.Inject

plugins {
  alias(libs.plugins.spring.boot)
  id("com.incode.service-conventions")
}

dependencies {
  // Platform
  implementation(platform(libs.spring.boot.bom))

  // Application runtime
  implementation(libs.spring.boot.starter.jdbc)
  implementation(libs.spring.boot.starter.data.redis)
  implementation(libs.spring.boot.starter.web)
  implementation(libs.spring.boot.starter.actuator)
  implementation(libs.micrometer.registry.prometheus)
  implementation(libs.spring.boot.starter.validation)
  implementation(libs.httpclient5)
  implementation(libs.uuid.creator)
  implementation(libs.resilience4j.spring.boot4)
  implementation(libs.caffeine)
  implementation(libs.jackson.databind)
  implementation(libs.flyway.core)
  implementation(libs.flyway.database.postgresql)

  // Runtime infrastructure
  runtimeOnly(libs.postgresql)

  // Build-time analysis
  checkstyle(libs.checkstyle)
  errorprone(libs.error.prone.core)

  // Tests
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.archunit.junit5)
  testImplementation(libs.spring.modulith.core)
  testRuntimeOnly(libs.junit.platform.launcher)
  testImplementation(libs.spring.boot.starter.test)
}

dependencyLocking { lockAllConfigurations() }

val performanceScenarioValues =
  layout.projectDirectory
    .file("performance/scenarios.env")
    .asFile
    .readLines()
    .map(String::trim)
    .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains('=') }
    .associate { line -> line.substringBefore('=').trim() to line.substringAfter('=').trim() }

fun performanceScenario(
  name: String,
  defaultValue: String,
) = performanceScenarioValues[name] ?: defaultValue

abstract class PerformanceTestTask
  @Inject
  constructor(
    private val execOperations: ExecOperations,
  ) : DefaultTask() {
    init {
      group = "verification"
      description = "Run the service-owned Locust workload using the parent Compose stack"
    }

    @TaskAction
    fun runPerformanceTest() {
      val workspaceDirectory = project.rootDir.parentFile
      val compose = listOf("docker", "compose", "--profile", "performance")

      fun runCompose(vararg arguments: String) {
        execOperations.exec {
          workingDir(workspaceDirectory)
          commandLine(compose + arguments)
        }
      }

      runCompose("up", "-d")
      try {
        runCompose(
          "run",
          "--rm",
          "locust",
          "--headless",
          "--users",
          performanceScenario("PERFORMANCE_USERS", "5"),
          "--spawn-rate",
          performanceScenario("PERFORMANCE_SPAWN_RATE", "1"),
          "--run-time",
          performanceScenario("PERFORMANCE_DURATION", "30s"),
          "--only-summary",
          "--html",
          "/mnt/artifacts/report.html",
          "--csv",
          "/mnt/artifacts/locust",
        )
      } finally {
        runCompose("down")
      }
    }
  }

tasks.register<PerformanceTestTask>("performanceTest")
