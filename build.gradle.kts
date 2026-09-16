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
    implementation(libs.spring.boot.starter.data.redis)
    implementation(libs.spring.boot.starter.web)
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation(libs.httpclient5)
    implementation(libs.resilience4j.spring.boot3)
    implementation(libs.caffeine)
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

val openApiContracts = fileTree(layout.projectDirectory.dir("openapi")) {
    include("**/*.yaml", "**/*.yml", "**/*.json")
}
val jacocoArtifactDirectory = layout.buildDirectory.dir("reports/jacoco")
val performanceArtifactDirectory = layout.buildDirectory.dir("reports/performance")
dependencyLocking { lockAllConfigurations() }
tasks.test { useJUnitPlatform() }
checkstyle { toolVersion = libs.versions.checkstyle.get() }
spotless { kotlinGradle { ktlint() } }

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    outputs.cacheIf { false }
    systemProperty("junit.jupiter.execution.parallel.enabled", "false")
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
    reportsDirectory.set(jacocoArtifactDirectory)
}

tasks.withType<JacocoCoverageVerification>().configureEach {
    dependsOn(tasks.test)
    violationRules {
        rule {
            element = "BUNDLE"
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

listOf("integrationTest", "contractTest", "e2eTest").forEach { suiteName ->
    tasks.matching { it.name == suiteName }.configureEach {
        outputs.cacheIf { false }
        outputs.upToDateWhen { false }
    }
}

val performanceTest = tasks.register("performanceTest") {
    group = "verification"
    description = "Runs the performance baseline through the external mise/Locust harness."
    outputs.cacheIf { false }
    outputs.dir(performanceArtifactDirectory)
    doLast {
        performanceArtifactDirectory.get().asFile.mkdirs()
    }
}

val openApiValidate = tasks.register("openApiValidate") {
    group = "verification"
    description = "Validates all checked-in OpenAPI contracts with the pinned Redocly CLI."
    inputs.files(openApiContracts).withPropertyName("openApiContracts")
    outputs.dir(layout.buildDirectory.dir("reports/openapi"))
    doLast {
        val reportDirectory = layout.buildDirectory.dir("reports/openapi").get().asFile
        reportDirectory.mkdirs()
        exec {
            commandLine("npx", "--yes", "@redocly/cli@1.34.0", "lint", *openApiContracts.files.map { it.path }.toTypedArray())
        }
    }
}

val imageVariant = providers.gradleProperty("imageVariant").orElse("jvm")
val paketoBuilderImage = providers.gradleProperty("paketoBuilderImage")
    .map { image -> requireDigestImage("paketoBuilderImage", image) }
val paketoRunImage = providers.gradleProperty("paketoRunImage")
    .map { image -> requireDigestImage("paketoRunImage", image) }
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
    inputs.property("paketoBuilderImage", paketoBuilderImage)
    inputs.property("paketoRunImage", paketoRunImage)
    inputs.file(layout.projectDirectory.file("gradle.lockfile"))
    inputs.file(layout.projectDirectory.file("settings-gradle.lockfile"))
    builder.set(paketoBuilderImage)
    runImage.set(paketoRunImage)
    environment.put("BP_IMAGE_LABELS", providers.provider {
        "org.opencontainers.image.title=company-check-service,org.opencontainers.image.source=https://github.com/incode/company-check-service,org.opencontainers.image.version=${project.version},org.opencontainers.image.vendor=Incode"
    })
    environment.put("BP_JVM_VERSION", providers.provider { java.toolchain.languageVersion.get().asInt().toString() })
    environment.put("BPE_DEFAULT_BPL_JVM_HEAD_ROOM", providers.provider { "10" })
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
    inputs.property("paketoBuilderImage", paketoBuilderImage)
    inputs.property("paketoRunImage", paketoRunImage)
    inputs.file(layout.projectDirectory.file("gradle.lockfile"))
    inputs.file(layout.projectDirectory.file("settings-gradle.lockfile"))
}

fun requireDigestImage(propertyName: String, image: String): String {
    require(image.matches(Regex("^[^@/]+(?:/[^@]+)*/[^@]+@sha256:[0-9a-fA-F]{64}$"))) {
        "$propertyName must be an immutable image reference ending in @sha256:<64 hex digits>"
    }
    return image
}
testing { suites { register<JvmTestSuite>("integrationTest") { useJUnitJupiter() }; register<JvmTestSuite>("contractTest") { useJUnitJupiter() }; register<JvmTestSuite>("e2eTest") { useJUnitJupiter() } } }
tasks.named("check") { dependsOn("integrationTest", "contractTest", "e2eTest") }

tasks.register("qualityGate") {
    group = "verification"
    description = "Runs the complete service quality gate."
    dependsOn("spotlessCheck", "checkstyleMain", "checkstyleTest", "test", "jacocoTestReport", "jacocoTestCoverageVerification", openApiValidate)
}
