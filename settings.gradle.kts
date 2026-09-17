import org.gradle.caching.http.HttpBuildCache

pluginManagement {
  includeBuild("build-logic")
  repositories {
    gradlePluginPortal()
    mavenCentral()
  }
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories { mavenCentral() }
}

fun booleanProperty(
  name: String,
  default: Boolean,
) = providers.gradleProperty(name).map(String::toBooleanStrict).orElse(default)

val localBuildCacheEnabled = booleanProperty("localBuildCache", true)
val localBuildCachePush = booleanProperty("localBuildCachePush", true)
val remoteBuildCacheUrl = providers.gradleProperty("remoteBuildCacheUrl")
val remoteBuildCachePush = booleanProperty("remoteBuildCachePush", false)
val remoteBuildCacheAllowInsecure = booleanProperty("remoteBuildCacheAllowInsecure", false)

buildCache {
  local {
    directory = File(rootDir, ".gradle/build-cache")
    isEnabled = localBuildCacheEnabled.get()
    isPush = localBuildCachePush.get()
  }

  if (remoteBuildCacheUrl.isPresent) {
    val cacheUrl = remoteBuildCacheUrl.get()
    require(remoteBuildCacheAllowInsecure.get() || cacheUrl.startsWith("https://")) {
      "remoteBuildCacheUrl must use HTTPS unless remoteBuildCacheAllowInsecure=true"
    }
    remote<HttpBuildCache> {
      url = uri(cacheUrl)
      isPush = remoteBuildCachePush.get()
      isAllowInsecureProtocol = remoteBuildCacheAllowInsecure.get()
      credentials {
        username = providers.gradleProperty("remoteBuildCacheUsername").orNull
        password = providers.gradleProperty("remoteBuildCachePassword").orNull
      }
    }
  }
}

rootProject.name = "company-check-service"
