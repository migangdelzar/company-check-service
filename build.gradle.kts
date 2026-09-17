import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.springframework.boot.gradle.tasks.bundling.BootJar

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

val imageVariant = providers.gradleProperty("imageVariant").orElse("jvm").get()

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

if (imageVariant == "jvm") {
  // The GraalVM plugin marks the executable JAR as native-processed even when
  // the JVM image path does not run AOT tasks. Remove that marker so Paketo
  // cannot select its native-image build plan for a JVM image.
  tasks.named<BootJar>("bootJar") {
    manifest.attributes.remove("Spring-Boot-Native-Processed")
  }
}

dependencies {
  // Platform and framework BOMs
  implementation(platform(libs.spring.boot.bom))

  // Application APIs and adapters
  implementation(libs.spring.boot.starter.jdbc)
  implementation(libs.spring.boot.starter.data.redis)
  implementation(libs.commons.pool2)
  implementation(libs.spring.boot.starter.web)
  implementation(libs.spring.boot.starter.actuator)
  implementation(libs.spring.boot.starter.opentelemetry)
  implementation(libs.spring.boot.starter.cache)
  implementation(libs.spring.boot.starter.aspectj)
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
  implementation(libs.spring.boot.flyway)
  runtimeOnly(libs.postgresql)

  // Compile-time annotations and builders
  compileOnly(libs.lombok)
  annotationProcessor(libs.lombok)
  testCompileOnly(libs.lombok)
  testAnnotationProcessor(libs.lombok)

  // Static analysis
  checkstyle(libs.checkstyle)
  checkstyle("org.codehaus.plexus:plexus-utils:3.6.1")
  errorprone(libs.error.prone.core)
  errorprone(libs.nullaway)

  // Tests
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.archunit.junit5)
  testRuntimeOnly(libs.junit.platform.launcher)
  testImplementation(libs.spring.boot.starter.test)
  testImplementation(libs.spring.boot.starter.webmvc.test)
  testImplementation(libs.spring.boot.starter.data.jdbc.test)
  testImplementation(libs.spring.boot.starter.data.redis.test)
}

dependencyLocking { lockAllConfigurations() }
