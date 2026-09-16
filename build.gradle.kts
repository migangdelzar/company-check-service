plugins {
  alias(libs.plugins.spring.boot)
  id("com.incode.service-conventions")
}

dependencies {
  // Platform
  implementation(platform(libs.spring.boot.bom))

  // Application runtime
  implementation(libs.spring.boot.starter.jdbc)
  implementation(libs.spring.boot.starter.data.redis)
  implementation(libs.spring.boot.starter.web)
  implementation(libs.spring.boot.starter.actuator)
  implementation(libs.micrometer.registry.prometheus)
  implementation(libs.spring.boot.starter.validation)
  implementation(libs.httpclient5)
  implementation(libs.uuid.creator)
  implementation(libs.resilience4j.spring.boot4)
  implementation(libs.caffeine)
  implementation(libs.jackson.databind)
  implementation(libs.flyway.core)
  implementation(libs.flyway.database.postgresql)

  // Runtime infrastructure
  runtimeOnly(libs.postgresql)

  // Build-time analysis
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
