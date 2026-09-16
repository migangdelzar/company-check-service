import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test

plugins {
  checkstyle
  jacoco
  id("com.diffplug.spotless")
  id("net.ltgt.errorprone")
}

val jacocoArtifactDirectory = layout.buildDirectory.dir("reports/jacoco")
val libsCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

jacoco { reportsDirectory.set(jacocoArtifactDirectory) }
checkstyle { toolVersion = libsCatalog.findVersion("checkstyle").get().requiredVersion }
spotless {
  java {
    googleJavaFormat(libsCatalog.findVersion("google-java-format").get().requiredVersion)
    removeUnusedImports()
  }
  kotlinGradle { ktlint() }
}

tasks.matching { task ->
  task.name.startsWith("checkstyle") || task.name.startsWith("jacoco")
}.configureEach {
  group = "quality"
}

tasks.named<JacocoReport>("jacocoTestReport") {
  dependsOn(tasks.named<Test>("test"))
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
  dependsOn(tasks.named<Test>("test"))
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

tasks.register("qualityGate") {
  group = "quality"
  description = "Runs the complete service quality gate."
  dependsOn(
    "spotlessCheck",
    "checkstyleMain",
    "checkstyleTest",
    "check",
    "test",
    "jacocoTestReport",
    "jacocoTestCoverageVerification",
    "openApiValidate",
  )
}

tasks.register("verifyFinalGates") {
  group = "quality"
  description = "Runs all service-owned final gates."
  dependsOn("qualityGate", "openApiValidate")
}
