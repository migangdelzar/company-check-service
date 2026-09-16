import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
  java
  id("java-test-fixtures")
}

val libsCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

java.toolchain.languageVersion.set(
  JavaLanguageVersion.of(libsCatalog.findVersion("java").get().requiredVersion.toInt())
)
