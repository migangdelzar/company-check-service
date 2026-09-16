plugins {
    java
    checkstyle
    jacoco
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spotless)
    alias(libs.plugins.error.prone)
    id("java-test-fixtures")
    id("jvm-test-suite")
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(25))

dependencies {
    implementation(libs.spring.boot.starter.jdbc)
    implementation(libs.jackson.databind)
    implementation(libs.flyway.core)
    implementation(libs.flyway.database.postgresql)
    runtimeOnly(libs.postgresql)
    checkstyle(libs.checkstyle)
    errorprone(libs.error.prone.core)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.archunit.junit5)
    testImplementation(libs.spring.modulith.core)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
}
dependencyLocking { lockAllConfigurations() }
tasks.test { useJUnitPlatform() }
checkstyle { toolVersion = libs.versions.checkstyle.get() }
spotless { kotlinGradle { ktlint() } }

val imageVariant = providers.gradleProperty("imageVariant").orElse("jvm")
val nativeOptimization = providers.gradleProperty("nativeOptimization")
    .orElse("b")
    .map { optimization ->
        require(optimization == "b") {
            "nativeOptimization must be: b"
        }
        optimization
    }
val validatedVariant = imageVariant.map { variant ->
    require(variant == "jvm" || variant == "native") {
        "imageVariant must be one of: jvm, native"
    }
    variant
}
val imageArchitecture = providers.environmentVariable("DOCKER_DEFAULT_PLATFORM")
    .orElse(providers.systemProperty("os.arch").orElse("unknown"))

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootBuildImage>("bootBuildImage") {
    inputs.property("imageVariant", validatedVariant)
    inputs.property("nativeOptimization", nativeOptimization)
    inputs.property("imageArchitecture", imageArchitecture)
    inputs.file(layout.projectDirectory.file("gradle.lockfile"))
    inputs.file(layout.projectDirectory.file("settings-gradle.lockfile"))
    environment.put("BP_NATIVE_IMAGE", validatedVariant.map { (it == "native").toString() })
    environment.putAll(validatedVariant.flatMap { variant ->
        if (variant == "native") {
            nativeOptimization.map { optimization ->
                mapOf("BP_NATIVE_IMAGE_BUILD_ARGUMENTS" to "-O$optimization")
            }
        } else {
            providers.provider { emptyMap() }
        }
    })
}

tasks.register("image") {
    group = "build"
    description = "Builds the JVM or native image with Paketo via Spring Boot."
    dependsOn("bootBuildImage")
    inputs.property("imageVariant", validatedVariant)
    inputs.property("nativeOptimization", nativeOptimization)
    inputs.property("imageArchitecture", imageArchitecture)
    inputs.file(layout.projectDirectory.file("gradle.lockfile"))
    inputs.file(layout.projectDirectory.file("settings-gradle.lockfile"))
}
testing { suites { register<JvmTestSuite>("integrationTest") { useJUnitJupiter() }; register<JvmTestSuite>("contractTest") { useJUnitJupiter() }; register<JvmTestSuite>("e2eTest") { useJUnitJupiter() } } }
tasks.named("check") { dependsOn("integrationTest", "contractTest", "e2eTest") }
