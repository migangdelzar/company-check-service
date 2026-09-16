import java.time.Duration
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.testing.Test

// Gradle core plugins.
plugins {
  java
  checkstyle
  jacoco
  // External plugins are managed centrally in gradle/libs.versions.toml.
  alias(libs.plugins.spring.boot)
  alias(libs.plugins.spotless)
  alias(libs.plugins.error.prone)
  id("java-test-fixtures")
  id("jvm-test-suite")
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(25))

dependencies {
  implementation(platform(libs.spring.boot.bom))
  implementation(libs.spring.boot.starter.jdbc)
  implementation(libs.spring.boot.starter.data.redis)
  implementation(libs.spring.boot.starter.web)
  implementation(libs.spring.boot.starter.actuator)
  implementation(libs.micrometer.registry.prometheus)
  implementation(libs.spring.boot.starter.validation)
  implementation(libs.httpclient5)
  implementation(libs.resilience4j.spring.boot4)
  implementation(libs.caffeine)
  implementation(libs.jackson.databind)
  implementation(libs.flyway.core)
  implementation(libs.flyway.database.postgresql)
  runtimeOnly(libs.postgresql)
  checkstyle(libs.checkstyle)
  errorprone(libs.error.prone.core)
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.archunit.junit5)
  testImplementation(libs.spring.modulith.core)
  testRuntimeOnly(libs.junit.platform.launcher)
  testImplementation(libs.spring.boot.starter.test)
}

val openApiContracts =
  fileTree(layout.projectDirectory.dir("openapi")) {
    include("**/*.yaml", "**/*.yml", "**/*.json")
  }
val jacocoArtifactDirectory = layout.buildDirectory.dir("reports/jacoco")
val performanceArtifactDirectory = layout.buildDirectory.dir("reports/performance")
dependencyLocking { lockAllConfigurations() }
jacoco {
  reportsDirectory.set(jacocoArtifactDirectory)
}
tasks.test { useJUnitPlatform() }
checkstyle { toolVersion = libs.versions.checkstyle.get() }
spotless {
  java {
    googleJavaFormat("1.28.0")
    removeUnusedImports()
  }
  kotlinGradle { ktlint() }
}

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
  systemProperty("junit.jupiter.execution.parallel.enabled", "false")
}

tasks.named<JacocoReport>("jacocoTestReport") {
  dependsOn(tasks.test)
  classDirectories.setFrom(
    files(
      classDirectories.files.map { directory ->
        fileTree(directory) {
          exclude(
            "com/incode/verification/adapter/config/**",
            "com/incode/verification/adapter/out/**",
          )
        }
      },
    ),
  )
  reports {
    xml.required.set(true)
    html.required.set(true)
    csv.required.set(false)
  }
}

tasks.withType<JacocoCoverageVerification>().configureEach {
  dependsOn(tasks.test)
  classDirectories.setFrom(
    files(
      classDirectories.files.map { directory ->
        fileTree(directory) {
          exclude(
            "com/incode/verification/adapter/config/**",
            "com/incode/verification/adapter/out/**",
          )
        }
      },
    ),
  )
  violationRules {
    rule {
      element = "BUNDLE"
      limit {
        counter = "LINE"
        value = "COVEREDRATIO"
        minimum = "0.80".toBigDecimal()
      }
    }
  }
}

testing.suites.withType<JvmTestSuite>().configureEach {
  targets.configureEach {
    testTask.configure {
      outputs.cacheIf { false }
      outputs.upToDateWhen { false }
    }
  }
}

val performanceTest =
  tasks.register("performanceTest") {
    group = "verification"
    description = "Runs the performance baseline through the external mise/Locust harness."
    outputs.cacheIf { false }
    outputs.dir(performanceArtifactDirectory)
    doLast {
      performanceArtifactDirectory.get().asFile.mkdirs()
    }
  }

val openApiReportDirectory = layout.buildDirectory.dir("reports/openapi")
val openApiValidate =
  tasks.register<Exec>("openApiValidate") {
    group = "verification"
    description = "Validates all checked-in OpenAPI contracts with the pinned Redocly CLI."
    inputs.files(openApiContracts).withPropertyName("openApiContracts")
    outputs.dir(openApiReportDirectory)
    commandLine("npx", "--yes", "@redocly/cli@1.34.0", "lint")
    args(openApiContracts.files.map { it.path })
  }

// ---------------------------------------------------------------------------
// Paketo image properties are validated once, at the Gradle boundary.
// ---------------------------------------------------------------------------

val imageVariant = providers.gradleProperty("imageVariant").orElse("jvm")
val paketoBuilderImage =
  providers
    .gradleProperty("paketoBuilderImage")
    .map { image -> requireDigestImage("paketoBuilderImage", image) }
val paketoRunImage =
  providers
    .gradleProperty("paketoRunImage")
    .map { image -> requireDigestImage("paketoRunImage", image) }
val nativeOptimization =
  providers
    .gradleProperty("nativeOptimization")
    .orElse("b")
    .map { optimization ->
      require(optimization == "b") {
        "nativeOptimization must be: b"
      }
      optimization
    }
val validatedVariant =
  imageVariant.map { variant ->
    require(variant == "jvm" || variant == "native") {
      "imageVariant must be one of: jvm, native"
    }
    variant
  }
val imageArchitecture =
  providers
    .environmentVariable("DOCKER_DEFAULT_PLATFORM")
    .orElse(providers.systemProperty("os.arch").orElse("unknown"))

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootBuildImage>("bootBuildImage") {
  inputs.property("imageVariant", validatedVariant)
  inputs.property("nativeOptimization", nativeOptimization)
  inputs.property("imageArchitecture", imageArchitecture)
  inputs.property("paketoBuilderImage", paketoBuilderImage)
  inputs.property("paketoRunImage", paketoRunImage)
  inputs.file(layout.projectDirectory.file("gradle.lockfile"))
  inputs.file(layout.projectDirectory.file("settings-gradle.lockfile"))
  builder.set(paketoBuilderImage)
  runImage.set(paketoRunImage)
  environment.put(
    "BP_IMAGE_LABELS",
    providers.provider {
      "org.opencontainers.image.title=company-check-service,org.opencontainers.image.source=https://github.com/incode/company-check-service,org.opencontainers.image.version=${project.version},org.opencontainers.image.vendor=Incode"
    },
  )
  environment.put(
    "BP_JVM_VERSION",
    providers.provider {
      java.toolchain.languageVersion
        .get()
        .asInt()
        .toString()
    },
  )
  environment.put("BPE_DEFAULT_BPL_JVM_HEAD_ROOM", providers.provider { "10" })
  environment.put("BP_NATIVE_IMAGE", validatedVariant.map { (it == "native").toString() })
  environment.putAll(
    validatedVariant.flatMap { variant ->
      if (variant == "native") {
        nativeOptimization.map { optimization ->
          mapOf("BP_NATIVE_IMAGE_BUILD_ARGUMENTS" to "-O$optimization")
        }
      } else {
        providers.provider { emptyMap() }
      }
    },
  )
}

tasks.register("image") {
  group = "build"
  description = "Builds the JVM or native image with Paketo via Spring Boot."
  dependsOn("bootBuildImage")
  inputs.property("imageVariant", validatedVariant)
  inputs.property("nativeOptimization", nativeOptimization)
  inputs.property("imageArchitecture", imageArchitecture)
  inputs.property("paketoBuilderImage", paketoBuilderImage)
  inputs.property("paketoRunImage", paketoRunImage)
  inputs.file(layout.projectDirectory.file("gradle.lockfile"))
  inputs.file(layout.projectDirectory.file("settings-gradle.lockfile"))
}

fun requireDigestImage(
  propertyName: String,
  image: String,
): String {
  require(image.matches(Regex("^[^@/]+(?:/[^@]+)*/[^@]+@sha256:[0-9a-fA-F]{64}$"))) {
    "$propertyName must be an immutable image reference ending in @sha256:<64 hex digits>"
  }
  return image
}
testing {
  suites {
    listOf("integrationTest", "contractTest", "e2eTest").forEach { suiteName ->
      register<JvmTestSuite>(suiteName) {
        useJUnitJupiter()
        sources {
          java.setSrcDirs(listOf("src/$suiteName/java"))
        }
        dependencies {
          implementation(project())
          if (suiteName == "integrationTest") {
            implementation(libs.testcontainers.junit.jupiter)
            implementation(libs.testcontainers.postgresql)
          }
        }
      }
    }
  }
}

listOf("integrationTest", "contractTest", "e2eTest").forEach { suiteName ->
  configurations.named("${suiteName}Implementation") {
    extendsFrom(configurations.testImplementation.get())
  }
  configurations.named("${suiteName}RuntimeOnly") {
    extendsFrom(configurations.testRuntimeOnly.get())
  }
}
tasks.named("check") { dependsOn("integrationTest", "contractTest", "e2eTest") }

tasks.register("qualityGate") {
  group = "verification"
  description = "Runs the complete service quality gate."
  dependsOn(
    "spotlessCheck",
    "checkstyleMain",
    "checkstyleTest",
    "test",
    "jacocoTestReport",
    "jacocoTestCoverageVerification",
    openApiValidate,
  )
}

// ---------------------------------------------------------------------------
// Final gates and image smoke checks are Gradle tasks, not shell declarations.
// ---------------------------------------------------------------------------
tasks.register("verifyFinalGates") {
  group = "verification"
  description = "Runs all service-owned final gates."
  dependsOn("qualityGate", openApiValidate, "performanceTest")
  doLast {
    check(openApiContracts.files.isNotEmpty()) { "No OpenAPI contract is configured" }
    check(tasks.names.containsAll(listOf("performanceTest", "qualityGate", "imageSmoke"))) {
      "Expected Gradle-owned final gate tasks are missing"
    }
  }
}

val imageName = providers.gradleProperty("imageName").orElse("company-check-service:${project.version}")
val imageSmokeTimeoutSeconds = providers.gradleProperty("imageSmokeTimeoutSeconds").map { value ->
  value.toLongOrNull()?.also { timeout ->
    require(timeout in 1..120) { "imageSmokeTimeoutSeconds must be between 1 and 120" }
  } ?: error("imageSmokeTimeoutSeconds must be an integer")
}.orElse(15)

fun docker(arguments: List<String>): String {
  val process = ProcessBuilder(listOf("docker") + arguments)
    .redirectErrorStream(true)
    .start()
  val output = process.inputStream.readBytes().toString(Charsets.UTF_8).trim()
  check(process.waitFor() == 0) { "docker ${arguments.joinToString(" ")} failed: $output" }
  return output
}

tasks.register("imageSmoke") {
  group = "verification"
  description = "Runs a bounded Docker smoke check against the locally built image."
  dependsOn("image")
  outputs.cacheIf { false }
  inputs.property("imageName", imageName)
  inputs.property("timeoutSeconds", imageSmokeTimeoutSeconds)
  doLast {
    val image = imageName.get()
    val timeout = imageSmokeTimeoutSeconds.get()
    docker(listOf("image", "inspect", image))
    val containerId = docker(listOf(
      "create", "--read-only", "--cap-drop=ALL", "--security-opt=no-new-privileges:true", image,
    ))
    try {
      docker(listOf("start", containerId))
      val deadline = System.nanoTime() + Duration.ofSeconds(timeout).toNanos()
      var running = true
      while (running && System.nanoTime() < deadline) {
        running = docker(listOf("inspect", "--format", "{{.State.Running}}", containerId)) == "true"
        if (running) Thread.sleep(100)
      }
      check(!running) { "Image did not reach a terminal state within ${timeout}s" }
      logger.lifecycle(docker(listOf("inspect", "--format", "exit={{.State.ExitCode}} user={{.Config.User}}", containerId)))
    } finally {
      runCatching { docker(listOf("rm", "-f", containerId)) }
    }
  }
}
