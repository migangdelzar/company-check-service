import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
  alias(libs.plugins.spring.boot)
  alias(libs.plugins.graalvm.native)
  java
  id("com.incode.testing-conventions")
  id("com.incode.quality-conventions")
  id("com.incode.contract-conventions")
  id("com.incode.container-conventions")
}

group = "com.incode.verification"

val javaLanguageVersion =
  JavaLanguageVersion.of(
    libs.versions.java
      .get()
      .toInt(),
  )

java.toolchain.languageVersion.set(javaLanguageVersion)

graalvmNative {
  // Keep the regular JVM toolchain independent from the native-image toolchain.
  // The Foojay resolver provisions a matching native-image-capable JDK on demand.
  toolchainDetection.set(true)
  binaries {
    named("main") {
      javaLauncher.set(
        javaToolchains.launcherFor {
          languageVersion.set(javaLanguageVersion)
          nativeImageCapable.set(true)
        },
      )
    }
  }
}

dependencies {
  // Platform and framework BOMs
  implementation(platform(libs.spring.boot.bom))

  // Application APIs and adapters
  implementation(libs.spring.boot.starter.jdbc)
  implementation(libs.spring.boot.starter.data.r2dbc)
  implementation(libs.r2dbc.pool)
  implementation(libs.spring.boot.starter.data.redis)
  implementation(libs.commons.pool2)
  implementation(libs.spring.boot.starter.webflux)
  implementation(libs.spring.boot.starter.actuator)
  implementation(libs.spring.boot.starter.opentelemetry)
  implementation(libs.spring.boot.starter.cache)
  implementation(libs.spring.boot.starter.aspectj)
  implementation(libs.spring.boot.starter.validation)
  implementation(libs.micrometer.registry.prometheus)
  implementation(libs.uuid.creator)
  implementation(libs.resilience4j.spring.boot4)
  implementation(libs.resilience4j.reactor)
  implementation(libs.caffeine)
  implementation(libs.jackson.databind)
  implementation(libs.jackson.datatype.jsr310)

  // Database migrations and runtime drivers
  implementation(libs.flyway.core)
  implementation(libs.flyway.database.postgresql)
  implementation(libs.spring.boot.flyway)
  runtimeOnly(libs.postgresql)
  implementation(libs.r2dbc.postgresql)

  // Compile-time annotations and builders
  compileOnly(libs.lombok)
  annotationProcessor(libs.lombok)
  testCompileOnly(libs.lombok)
  testAnnotationProcessor(libs.lombok)

  // Static analysis
  checkstyle(libs.checkstyle)
  checkstyle(libs.plexus.utils)
  errorprone(libs.error.prone.core)
  errorprone(libs.nullaway)

  // Tests
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.archunit.junit5)
  testImplementation(libs.reactor.test)
  testRuntimeOnly(libs.junit.platform.launcher)
  testImplementation(libs.spring.boot.starter.test)
  testImplementation(libs.spring.boot.starter.webflux.test)
  testImplementation(libs.spring.boot.starter.data.redis.test)
}

dependencyLocking { lockAllConfigurations() }
