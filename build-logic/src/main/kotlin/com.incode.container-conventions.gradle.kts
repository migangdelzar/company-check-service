import org.springframework.boot.gradle.tasks.bundling.BootBuildImage
import org.springframework.boot.gradle.tasks.bundling.BootJar
import java.time.Duration

// Image configuration
val configuredImageName =
  providers.gradleProperty("imageName").orElse("company-check-service:${project.version}")

// Paketo configuration
val publishImage =
  providers
    .gradleProperty("publishImage")
    .map { value ->
      value.toBooleanStrictOrNull() ?: error("publishImage must be true or false")
    }.orElse(false)
val paketoCacheVolumePrefix =
  providers.gradleProperty("paketoCacheVolumePrefix").orElse("company-check-service").map { prefix ->
    require(prefix.matches(Regex("^[a-z0-9][a-z0-9_.-]{0,62}$"))) {
      "paketoCacheVolumePrefix must contain only lowercase letters, digits, dots, underscores, or hyphens"
    }
    prefix
  }
val paketoPullPolicy =
  providers.gradleProperty("paketoPullPolicy").orElse("IF_NOT_PRESENT").map { policy ->
    require(policy in setOf("ALWAYS", "IF_NOT_PRESENT", "NEVER")) {
      "paketoPullPolicy must be ALWAYS, IF_NOT_PRESENT, or NEVER"
    }
    policy
  }
val cleanPaketoCache =
  providers
    .gradleProperty("cleanPaketoCache")
    .map { value ->
      value.toBooleanStrictOrNull() ?: error("cleanPaketoCache must be true or false")
    }.orElse(false)
val validatedVariant =
  providers.gradleProperty("imageVariant").orElse("jvm").map { variant ->
    require(variant == "jvm" || variant == "native") {
      "imageVariant must be one of: jvm, native"
    }
    variant
  }

val requestedImagePlatform =
  providers.gradleProperty("imagePlatform").orNull?.also { platform ->
    require(platform.matches(Regex("^linux/(amd64|arm64)$"))) {
      "imagePlatform must be linux/amd64 or linux/arm64"
    }
  }

if (validatedVariant.get() == "jvm") {
  // Keep the JVM image as a regular executable JAR. The GraalVM plugin wires
  // Spring AOT tasks into the image lifecycle, which activates native-image
  // metadata even when the native-image buildpack is not requested.
  tasks
    .matching { task ->
      task.name in
        setOf(
          "aotClasses",
          "collectReachabilityMetadata",
          "compileAotJava",
          "processAot",
          "processAotResources",
        )
    }.configureEach {
      enabled = false
    }
  // The GraalVM plugin marks the executable JAR as native-processed even when
  // the JVM image path does not run AOT tasks. Remove that marker so Paketo
  // cannot select its native-image build plan for a JVM image.
  tasks.named<BootJar>("bootJar") {
    manifest.attributes.remove("Spring-Boot-Native-Processed")
  }
}

// Build metadata
val imageLabels =
  listOf(
    "org.opencontainers.image.title=company-check-service",
    "org.opencontainers.image.source=https://github.com/incode/company-check-service",
    "org.opencontainers.image.version=${project.version}",
    "org.opencontainers.image.vendor=Incode",
  ).joinToString(",")

tasks.named<BootBuildImage>("bootBuildImage") {
  group = "containers"
  imageName.set(configuredImageName)
  requestedImagePlatform?.let { imagePlatform.set(it) }
  publish.set(publishImage)
  setPullPolicy(paketoPullPolicy.get())
  cleanCache.set(cleanPaketoCache)
  buildCache {
    volume {
      name.set(paketoCacheVolumePrefix.map { "$it.build" })
    }
  }
  launchCache {
    volume {
      name.set(paketoCacheVolumePrefix.map { "$it.launch" })
    }
  }
  buildWorkspace {
    volume {
      name.set(paketoCacheVolumePrefix.map { "$it.workspace" })
    }
  }
  environment.put(
    "BP_IMAGE_LABELS",
    providers.provider { imageLabels },
  )
  environment.put("BPE_DEFAULT_BPL_JVM_HEAD_ROOM", providers.provider { "10" })
  if (validatedVariant.get() == "native") {
    environment.put("BP_NATIVE_IMAGE", "true")
    environment.put("BP_SPRING_AOT_ENABLED", "true")
    environment.put("BP_NATIVE_IMAGE_BUILD_ARGUMENTS", "-Ob")
  } else {
    // Spring Boot's image task defaults BP_NATIVE_IMAGE to true when the
    // GraalVM plugin is present. Override it for the regular JVM image.
    environment.put("BP_NATIVE_IMAGE", "false")
    environment.put("BP_SPRING_AOT_ENABLED", "false")
    // Force a regular JDK runtime here so the executable-jar process can find java.
    environment.put("BP_JVM_TYPE", "JDK")
  }
}

fun requireDigestImage(
  propertyName: String,
  image: String,
): String {
  require(image.matches(Regex("^[^@\\s]+@sha256:[0-9a-fA-F]{64}$"))) {
    "$propertyName must be an immutable image reference ending in @sha256:<64 hex digits>"
  }
  return image
}

val composeImageVariables =
  listOf(
    "COMPANY_CHECK_SERVICE_IMAGE",
    "COMPANY_CHECK_PROVIDER_IMAGE",
    "POSTGRES_IMAGE",
    "REDIS_IMAGE",
    "LOCUST_IMAGE",
  )

tasks.register("composeDigestCheck") {
  group = "containers"
  description = "Checks that all Compose image inputs are immutable digest references."
  inputs.files(
    listOf("compose.yaml", "compose.single.yaml", "compose.distributed.yaml")
      .map { rootProject.layout.projectDirectory.file("../$it") },
  )
  doLast {
    composeImageVariables.forEach { variable ->
      val value =
        providers.environmentVariable(variable).orElse(providers.gradleProperty(variable)).orNull
      require(value != null) { "$variable must be supplied for Compose digest validation" }
      requireDigestImage(variable, value)
    }
  }
}

val imageSmokeTimeoutSeconds =
  providers
    .gradleProperty("imageSmokeTimeoutSeconds")
    .map { value ->
      value.toLongOrNull()?.also { timeout ->
        require(timeout in 1..120) { "imageSmokeTimeoutSeconds must be between 1 and 120" }
      } ?: error("imageSmokeTimeoutSeconds must be an integer")
    }.orElse(15)

fun docker(arguments: List<String>): String {
  val process =
    ProcessBuilder(listOf("docker") + arguments)
      .redirectErrorStream(true)
      .start()
  val output =
    process.inputStream
      .readBytes()
      .toString(Charsets.UTF_8)
      .trim()
  check(process.waitFor() == 0) {
    "docker ${arguments.joinToString(" ")} failed: $output"
  }
  return output
}

tasks.register("imageSmoke") {
  group = "containers"
  description = "Runs a bounded Docker smoke check against the locally built image."
  notCompatibleWithConfigurationCache(
    "Docker process execution is intentionally isolated from configuration-cache serialization.",
  )
  dependsOn("bootBuildImage")
  outputs.cacheIf { false }
  inputs.property("imageName", configuredImageName)
  inputs.property("timeoutSeconds", imageSmokeTimeoutSeconds)
  doLast {
    val image = configuredImageName.get()
    val timeout = imageSmokeTimeoutSeconds.get()
    docker(listOf("image", "inspect", image))
    val containerId =
      docker(
        listOf(
          "create",
          "--read-only",
          "--cap-drop=ALL",
          "--security-opt=no-new-privileges:true",
          image,
        ),
      )
    try {
      docker(listOf("start", containerId))
      val deadline = System.nanoTime() + Duration.ofSeconds(timeout).toNanos()
      var running = true
      while (running && System.nanoTime() < deadline) {
        running = docker(listOf("inspect", "--format", "{{.State.Running}}", containerId)) == "true"
        if (running) Thread.sleep(100)
      }
      check(!running) { "Image did not reach a terminal state within ${timeout}s" }
      logger.lifecycle(
        docker(listOf("inspect", "--format", "exit={{.State.ExitCode}} user={{.Config.User}}", containerId)),
      )
    } finally {
      runCatching { docker(listOf("rm", "-f", containerId)) }
    }
  }
}

tasks.register("containerCheck") {
  group = "verification"
  description = "Builds the configured image and runs the bounded image smoke check."
  dependsOn("imageSmoke")
}
