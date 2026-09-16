plugins { java; checkstyle; jacoco; id("java-test-fixtures"); id("jvm-test-suite") }
java { toolchain { languageVersion.set(JavaLanguageVersion.of(25)) } }
dependencies { testImplementation(libs.junit.jupiter); testRuntimeOnly(libs.junit.platform.launcher); checkstyle(libs.checkstyle) }
dependencyLocking { lockAllConfigurations() }
tasks.test { useJUnitPlatform() }
checkstyle { toolVersion = libs.versions.checkstyle.get() }
val imageVariant = providers.gradleProperty("imageVariant").orElse("jvm")
val nativeOptimization = providers.gradleProperty("nativeOptimization").orElse("b")
tasks.register("image") { inputs.property("imageVariant", imageVariant); inputs.property("nativeOptimization", nativeOptimization); doFirst { require(imageVariant.get() in listOf("jvm", "native")) { "imageVariant must be one of: jvm, native" }; require(nativeOptimization.get() == "b") { "nativeOptimization must be: b" } } }
testing { suites { register<JvmTestSuite>("integrationTest") { useJUnitJupiter() }; register<JvmTestSuite>("contractTest") { useJUnitJupiter() }; register<JvmTestSuite>("e2eTest") { useJUnitJupiter() } } }
tasks.named("check") { dependsOn("integrationTest", "contractTest", "e2eTest") }
