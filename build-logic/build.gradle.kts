import org.gradle.api.artifacts.VersionCatalogsExtension

plugins { `kotlin-dsl` }

repositories {
  gradlePluginPortal()
  mavenCentral()
}

val libsCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
  implementation(libsCatalog.findLibrary("spring-boot-gradle-plugin").get())
  implementation(libsCatalog.findLibrary("spotless-gradle-plugin").get())
  implementation(libsCatalog.findLibrary("error-prone-gradle-plugin").get())
}
