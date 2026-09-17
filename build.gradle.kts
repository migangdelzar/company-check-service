import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
  alias(libs.plugins.spring.boot)
  java
  id("com.incode.testing-conventions")
  id("com.incode.quality-conventions")
  id("com.incode.contract-conventions")
  id("com.incode.container-conventions")
}

java.toolchain.languageVersion.set(
  JavaLanguageVersion.of(
    libs.versions.java
      .get()
      .toInt(),
  ),
)

dependencies {
  // Platform and framework BOMs
  implementation(platform(libs.spring.boot.bom))

  // Application APIs and adapters
  implementation(libs.spring.boot.starter.jdbc)
  implementation(libs.spring.boot.starter.data.redis)
  implementation(libs.spring.boot.starter.web)
  implementation(libs.spring.boot.starter.actuator)
  implementation(libs.spring.boot.starter.validation)
  implementation(libs.micrometer.registry.prometheus)
  implementation(libs.httpclient5)
  implementation(libs.uuid.creator)
  implementation(libs.resilience4j.spring.boot4)
  implementation(libs.caffeine)
  implementation(libs.jackson.databind)

  // Database migrations and runtime drivers
  implementation(libs.flyway.core)
  implementation(libs.flyway.database.postgresql)
  runtimeOnly(libs.postgresql)

  // Compile-time code generation only
  compileOnly(libs.lombok)
  annotationProcessor(libs.lombok)
  testCompileOnly(libs.lombok)
  testAnnotationProcessor(libs.lombok)

  // Static analysis
  checkstyle(libs.checkstyle)
  errorprone(libs.error.prone.core)

  // Tests
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.archunit.junit5)
  testImplementation(libs.spring.modulith.core)
  testRuntimeOnly(libs.junit.platform.launcher)
  testImplementation(libs.spring.boot.starter.test)
}

dependencyLocking { lockAllConfigurations() }
