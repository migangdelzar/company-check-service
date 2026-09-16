import org.gradle.api.tasks.Exec

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

tasks.register<Exec>("performanceTest") {
  group = "verification"
  description = "Run the service-owned Locust workload using the parent Compose stack"
  workingDir(rootDir.parentFile)
  commandLine(
    "docker",
    "compose",
    "--profile",
    "performance",
    "run",
    "--rm",
    "locust",
    "--headless",
    "-f",
    "/mnt/performance/locustfile.py",
    "--host",
    "http://backend:8080",
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
}
