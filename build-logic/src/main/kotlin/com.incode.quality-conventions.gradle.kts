import net.ltgt.gradle.errorprone.errorprone
import net.ltgt.gradle.nullaway.nullaway
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
  checkstyle
  jacoco
  id("com.diffplug.spotless")
  id("net.ltgt.errorprone")
  id("net.ltgt.nullaway")
}

val jacocoArtifactDirectory = layout.buildDirectory.dir("reports/jacoco")
val libsCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
val buildLogicCheckReference =
  gradle.includedBuilds
    .firstOrNull { includedBuild -> includedBuild.projectDir == rootProject.file("build-logic") }
    ?.task(":check")
val unitTest = tasks.named<Test>("test")
val coverageExcludedPaths =
  listOf(
    "com/incode/verification/client/**",
    "com/incode/verification/config/**",
    "com/incode/verification/repository/**",
  )

jacoco { reportsDirectory.set(jacocoArtifactDirectory) }
checkstyle { toolVersion = libsCatalog.findVersion("checkstyle").get().requiredVersion }
spotless {
  java {
    target("src/**/*.java")
    googleJavaFormat(libsCatalog.findVersion("google-java-format").get().requiredVersion)
    removeUnusedImports()
    trimTrailingWhitespace()
    endWithNewline()
  }
  kotlinGradle { ktlint(libsCatalog.findVersion("ktlint").get().requiredVersion) }
}

nullaway {
  annotatedPackages.add("com.incode")
}

tasks.withType<JavaCompile>().configureEach {
  options.encoding = "UTF-8"
}

tasks.named<JavaCompile>("compileJava") {
  options.compilerArgs.add("-XDaddTypeAnnotationsToSymbol=true")
  options.errorprone.nullaway { error() }
}

tasks
  .matching { task ->
    task.name.startsWith("checkstyle") || task.name.startsWith("jacoco")
  }.configureEach {
    group = "quality"
  }

// GraalVM's AOT task checks generated framework sources, which are outside the
// service's formatting and style boundary.
tasks.matching { task -> task.name == "checkstyleAot" }.configureEach { enabled = false }
tasks.matching { task -> task.name == "checkstyleAotTest" }.configureEach { enabled = false }

tasks.register("buildLogicCheck") {
  group = "quality"
  description = "Checks the service-owned Gradle convention plugins."
  buildLogicCheckReference?.let { dependsOn(it) }
}

tasks.named<JacocoReport>("jacocoTestReport") {
  dependsOn(unitTest)
  classDirectories.setFrom(
    files(
      classDirectories.files.map { directory ->
        fileTree(directory) {
          exclude(coverageExcludedPaths)
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
  dependsOn(unitTest)
  classDirectories.setFrom(
    files(
      classDirectories.files.map { directory ->
        fileTree(directory) {
          exclude(coverageExcludedPaths)
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

tasks.register("unitCheck") {
  group = "verification"
  description = "Runs formatting, static analysis, unit tests, and coverage checks."
  dependsOn(
    "spotlessCheck",
    "checkstyleMain",
    "checkstyleTest",
    "checkstyleTestFixtures",
    "test",
    "jacocoTestReport",
    "jacocoTestCoverageVerification",
  )
}

tasks.named("check") {
  dependsOn(
    "spotlessCheck",
    "checkstyleMain",
    "checkstyleTest",
    "checkstyleTestFixtures",
    "test",
    "jacocoTestReport",
    "jacocoTestCoverageVerification",
  )
}

val qualityGate =
  tasks.register("qualityGate") {
    group = "quality"
    description = "Runs the complete service quality gate."
    dependsOn("check", "openApiValidate", "buildLogicCheck")
  }

tasks.register("verifyFinalGates") {
  group = "quality"
  description = "Runs all service-owned final gates."
  dependsOn(qualityGate)
}
