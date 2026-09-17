import org.gradle.caching.http.HttpBuildCache

pluginManagement {
  includeBuild("build-logic")
  repositories {
    gradlePluginPortal()
    mavenCentral()
  }
}
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories { mavenCentral() }
}

val localBuildCacheEnabled =
  providers.gradleProperty("localBuildCache").map(String::toBooleanStrict).orElse(true)
val localBuildCachePush =
  providers.gradleProperty("localBuildCachePush").map(String::toBooleanStrict).orElse(true)
val remoteBuildCacheUrl = providers.gradleProperty("remoteBuildCacheUrl")
val remoteBuildCachePush =
  providers.gradleProperty("remoteBuildCachePush").map(String::toBooleanStrict).orElse(false)
val remoteBuildCacheAllowInsecure =
  providers.gradleProperty("remoteBuildCacheAllowInsecure").map(String::toBooleanStrict).orElse(false)

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
