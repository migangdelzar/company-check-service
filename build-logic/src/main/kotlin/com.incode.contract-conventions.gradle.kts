import org.gradle.api.tasks.Exec
import org.gradle.api.artifacts.VersionCatalogsExtension

plugins { base }

val openApiContracts =
  fileTree(layout.projectDirectory.dir("openapi")) {
    include("**/*.yaml", "**/*.yml", "**/*.json")
  }
val openApiReportDirectory = layout.buildDirectory.dir("reports/openapi")
val libsCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

tasks.register<Exec>("openApiValidate") {
  group = "contracts"
  description = "Validates all checked-in OpenAPI contracts with the pinned Redocly CLI."
  inputs.files(openApiContracts).withPropertyName("openApiContracts")
  outputs.dir(openApiReportDirectory)
  commandLine(
    "npx",
    "--yes",
    "@redocly/cli@${libsCatalog.findVersion("redocly-cli").get().requiredVersion}",
    "lint",
  )
  args(openApiContracts.files.map { it.path })
}
