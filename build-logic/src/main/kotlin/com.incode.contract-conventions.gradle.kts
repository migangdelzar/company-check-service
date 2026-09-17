import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.PathSensitivity

plugins { base }

val openApiContracts =
  fileTree(layout.projectDirectory.dir("openapi")) {
    include("**/*.yaml", "**/*.yml", "**/*.json")
  }
val openApiReportDirectory = layout.buildDirectory.dir("reports/openapi")
val libsCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
val redoclyCliVersion = libsCatalog.findVersion("redocly-cli").get().requiredVersion

tasks.register<Exec>("openApiValidate") {
  group = "contracts"
  description = "Validates all checked-in OpenAPI contracts with the pinned Redocly CLI."
  inputs
    .files(openApiContracts)
    .withPropertyName("openApiContracts")
    .withPathSensitivity(PathSensitivity.RELATIVE)
  inputs.property("redoclyCliVersion", redoclyCliVersion)
  outputs.dir(openApiReportDirectory)
  commandLine(
    "npx",
    "--yes",
    "@redocly/cli@$redoclyCliVersion",
    "lint",
  )
  args(openApiContracts.files.sortedBy { it.absolutePath }.map { it.absolutePath })
}
