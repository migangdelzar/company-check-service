pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
  }
}

dependencyResolutionManagement {
  repositories { mavenCentral() }
  versionCatalogs {
    create("libs") { from(files("../gradle/libs.versions.toml")) }
  }
}

rootProject.name = "company-check-service-build-logic"
