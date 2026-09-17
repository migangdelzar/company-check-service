import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jlleitschuh.gradle.ktlint.tasks.BaseKtLintCheckTask

plugins {
  `kotlin-dsl`
  alias(libs.plugins.detekt)
  alias(libs.plugins.ktlint)
}

repositories {
  gradlePluginPortal()
  mavenCentral()
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

ktlint {
  version.set(libsCatalog.findVersion("ktlint").get().requiredVersion)
  outputToConsole.set(true)
  ignoreFailures.set(false)
  filter {
    exclude("**/build/**")
    exclude("**/generated/**")
    exclude("**/generated-sources/**")
  }
}

tasks.withType<BaseKtLintCheckTask>().configureEach {
  when {
    name.contains("MainSourceSet") ->
      setSource(
        fileTree(layout.projectDirectory.dir("src/main/kotlin")) {
          include("**/*.kt", "**/*.kts")
        },
      )
    name.contains("TestSourceSet") ->
      setSource(
        fileTree(layout.projectDirectory.dir("src/test/kotlin")) {
          include("**/*.kt", "**/*.kts")
        },
      )
  }
}
