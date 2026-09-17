import io.gitlab.arturbosch.detekt.Detekt
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test

plugins {
  `kotlin-dsl`
  alias(libs.plugins.detekt)
  alias(libs.plugins.spotless)
}

dependencies {
  implementation(libs.spring.boot.gradle.plugin)
  implementation(libs.spotless.gradle.plugin)
  implementation(libs.error.prone.gradle.plugin)
  implementation(libs.nullaway.gradle.plugin)
  testImplementation(gradleTestKit())
  testImplementation(libs.junit.jupiter)
  testRuntimeOnly(libs.junit.platform.launcher)
}

detekt {
  buildUponDefaultConfig = true
  config.setFrom(files("config/detekt/detekt.yml"))
  parallel = true
}

// Detekt currently supports JVM targets through 22 even when the build runs on
// a newer Java toolchain. Keep the analyzer target compatible with its CLI.
tasks.withType<Detekt>().configureEach {
  jvmTarget =
    libs.versions.detekt.jvm.target
      .get()
}

spotless {
  kotlin {
    target("src/**/*.kt")
    ktlint(libs.versions.ktlint.get())
    trimTrailingWhitespace()
    endWithNewline()
  }
  kotlinGradle {
    target("*.gradle.kts", "src/**/*.gradle.kts")
    ktlint(libs.versions.ktlint.get())
    trimTrailingWhitespace()
    endWithNewline()
  }
}

tasks.withType<JavaCompile>().configureEach {
  options.encoding = "UTF-8"
  options.isIncremental = true
}

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
  systemProperty("buildLogicRoot", rootDir.absolutePath)
}

tasks.named("check") {
  dependsOn("spotlessCheck", "detekt")
}
