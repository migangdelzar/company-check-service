import org.gradle.api.tasks.Exec

val performanceScenarioValues =
  layout.projectDirectory
    .file("performance/scenarios.env")
    .asFile
    .readLines()
    .map(String::trim)
    .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains('=') }
    .associate { line -> line.substringBefore('=').trim() to line.substringAfter('=').trim() }

fun performanceScenario(name: String, defaultValue: String) =
  providers.gradleProperty(name).orElse(performanceScenarioValues[name] ?: defaultValue)

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
    performanceScenario("performanceUsers", "5").get(),
    "--spawn-rate",
    performanceScenario("performanceSpawnRate", "1").get(),
    "--run-time",
    performanceScenario("performanceDuration", "30s").get(),
    "--only-summary",
    "--html",
    "/mnt/artifacts/report.html",
    "--csv",
    "/mnt/artifacts/locust",
  )
}
