import org.gradle.api.plugins.JavaPluginExtension
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage
import java.time.Duration

// Image configuration
val imageVariant = providers.gradleProperty("imageVariant").orElse("jvm")
val configuredImageName =
  providers.gradleProperty("imageName").orElse("company-check-service:${project.version}")

// Paketo configuration
val publishImage =
  providers
    .gradleProperty("publishImage")
    .map { value ->
      value.toBooleanStrictOrNull() ?: error("publishImage must be true or false")
    }.orElse(false)
val configuredImagePlatform =
  providers
    .gradleProperty("imagePlatform")
    .map { platform ->
      require(platform.matches(Regex("^linux/(amd64|arm64)$"))) {
        "imagePlatform must be linux/amd64 or linux/arm64"
      }
      platform
    }.orElse("linux/amd64")
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
val paketoBuilderImage =
  providers.gradleProperty("paketoBuilderImage").map { image ->
    requireDigestImage("paketoBuilderImage", image)
  }
val paketoRunImage =
  providers.gradleProperty("paketoRunImage").map { image ->
    requireDigestImage("paketoRunImage", image)
  }

// Requested-task validation
val imageTaskRequested =
  gradle.startParameter.taskNames.any { task ->
    task.substringAfterLast(':') in setOf("image", "imageSmoke", "containerCheck", "bootBuildImage")
  }
if (imageTaskRequested) {
  require(paketoBuilderImage.isPresent) {
    "paketoBuilderImage is required and must be a digest-pinned image"
  }
  require(paketoRunImage.isPresent) {
    "paketoRunImage is required and must be a digest-pinned image"
  }
}
val nativeOptimization =
  providers.gradleProperty("nativeOptimization").orElse("b").map { optimization ->
    require(optimization == "b") { "nativeOptimization must be: b" }
    optimization
  }
val validatedVariant =
  imageVariant.map { variant ->
    require(variant == "jvm" || variant == "native") {
      "imageVariant must be one of: jvm, native"
    }
    variant
  }

// Build metadata
val javaExtension = extensions.getByType<JavaPluginExtension>()
val imageLabels =
  listOf(
    "org.opencontainers.image.title=company-check-service",
    "org.opencontainers.image.source=https://github.com/incode/company-check-service",
    "org.opencontainers.image.version=${project.version}",
    "org.opencontainers.image.vendor=Incode",
  ).joinToString(",")

fun org.gradle.api.Task.imageInputs() {
  inputs.properties(
    mapOf(
      "imageVariant" to validatedVariant,
      "imageName" to configuredImageName,
      "publishImage" to publishImage,
      "imagePlatform" to configuredImagePlatform,
      "paketoCacheVolumePrefix" to paketoCacheVolumePrefix,
      "paketoPullPolicy" to paketoPullPolicy,
      "cleanPaketoCache" to cleanPaketoCache,
      "nativeOptimization" to nativeOptimization,
      "paketoBuilderImage" to paketoBuilderImage,
      "paketoRunImage" to paketoRunImage,
    ),
  )
  inputs.files(
    layout.projectDirectory.file("gradle.lockfile"),
    layout.projectDirectory.file("settings-gradle.lockfile"),
  )
}

tasks.named<BootBuildImage>("bootBuildImage") {
  group = "containers"
  imageName.set(configuredImageName)
  publish.set(publishImage)
  imagePlatform.set(configuredImagePlatform)
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
  imageInputs()
  builder.set(paketoBuilderImage)
  runImage.set(paketoRunImage)
  environment.put(
    "BP_IMAGE_LABELS",
    providers.provider { imageLabels },
  )
  environment.put(
    "BP_JVM_VERSION",
    providers.provider {
      javaExtension.toolchain.languageVersion
        .get()
        .asInt()
        .toString()
    },
  )
  environment.put("BPE_DEFAULT_BPL_JVM_HEAD_ROOM", providers.provider { "10" })
  if (validatedVariant.get() == "native") {
    environment.put("BP_NATIVE_IMAGE", "true")
    environment.put("BP_SPRING_AOT_ENABLED", "true")
  } else {
    // The GraalVM plugin contributes native/AOT metadata even for JVM image builds.
    // Do not set BP_NATIVE_IMAGE: its presence activates Paketo's native-image buildpack.
    environment.put("BP_SPRING_AOT_ENABLED", "false")
    // The JVM build uses a native-capable Gradle toolchain for the native variant.
    // Force a regular JDK runtime here so the executable-jar process can find java.
    environment.put("BP_JVM_TYPE", "JDK")
  }
  environment.putAll(
    validatedVariant.flatMap { variant ->
      if (variant == "native") {
        nativeOptimization.map { optimization ->
          mapOf("BP_NATIVE_IMAGE_BUILD_ARGUMENTS" to "-O$optimization")
        }
      } else {
        providers.provider { emptyMap() }
      }
    },
  )
}

tasks.register("image") {
  group = "containers"
  description = "Builds the JVM or native image with Paketo via Spring Boot."
  dependsOn("bootBuildImage")
  imageInputs()
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
  dependsOn("image")
  outputs.cacheIf { false }
  inputs.property("imageName", configuredImageName)
  inputs.property("imagePlatform", configuredImagePlatform)
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
