import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.compile.JavaCompile

plugins {
  `kotlin-dsl`
  alias(libs.plugins.detekt)
  alias(libs.plugins.spotless)
}

val libsCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
  implementation(libsCatalog.findLibrary("spring-boot-gradle-plugin").get())
  implementation(libsCatalog.findLibrary("spotless-gradle-plugin").get())
  implementation(libsCatalog.findLibrary("error-prone-gradle-plugin").get())
  implementation(libsCatalog.findLibrary("nullaway-gradle-plugin").get())
}

detekt {
  buildUponDefaultConfig = true
  config.setFrom(files("config/detekt/detekt.yml"))
  parallel = true
}

spotless {
  kotlin {
    target("src/**/*.kt")
    ktlint(libsCatalog.findVersion("ktlint").get().requiredVersion)
    trimTrailingWhitespace()
    endWithNewline()
  }
  kotlinGradle {
    target("*.gradle.kts", "src/**/*.gradle.kts")
    ktlint(libsCatalog.findVersion("ktlint").get().requiredVersion)
    trimTrailingWhitespace()
    endWithNewline()
  }
}

tasks.withType<JavaCompile>().configureEach {
  options.encoding = "UTF-8"
  options.isIncremental = true
}

tasks.named("check") {
  dependsOn("spotlessCheck", "detekt")
}
