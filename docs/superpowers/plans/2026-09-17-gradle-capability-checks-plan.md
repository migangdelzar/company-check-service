# Gradle Capability Checks Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (\`- [ ]\`) syntax for tracking.

**Goal:** Make the existing Gradle build capabilities explicit, focused, and executable while preserving the current \`fastCheck\` and \`qualityGate\` behavior.

**Architecture:** Keep one deployable Gradle project and the existing \`build-logic\` included build. Add thin convention-plugin task aliases for unit, integration, contract, E2E, and container checks. Test actual convention-plugin behavior with isolated Gradle TestKit fixture builds; do not add a custom capability DSL or business modules.

**Tech Stack:** Gradle 9.0.0, Kotlin DSL precompiled convention plugins, Gradle TestKit, Java 25, JUnit Jupiter, existing \`jvm-test-suite\`, Spotless, Checkstyle, JaCoCo, OpenAPI validation, and Spring Boot container tasks.

## Global Constraints

- Keep \`company-check-service\` as one deployable Gradle project.
- Do not add business-capability subprojects, Gradle feature variants, or a custom \`capabilities {}\` extension.
- Preserve the current meaning of \`fastCheck\`: no integration, contract, E2E, OpenAPI, or container execution.
- Preserve the current complete \`qualityGate\` dependency graph and external-service behavior.
- Keep dependency versions in \`gradle/libs.versions.toml\`.
- Keep dependency locking, strict dependency verification, configuration cache, and build cache enabled.
- Use Gradle TestKit for executable build-logic behavior tests; retain source-structure tests only as smoke coverage.
- Preserve unrelated working-tree changes.
- Follow RED → GREEN → REFACTOR for each behavior change.

---

## File and Task Map

| File | Responsibility |
|---|---|
| \`build-logic/build.gradle.kts\` | TestKit and JUnit dependencies plus test-system properties |
| \`build-logic/src/test/kotlin/com/incode/buildlogic/CapabilityTaskFunctionalTest.kt\` | Isolated real-Gradle tests for aliases and gate relationships |
| \`build-logic/src/main/kotlin/com.incode.quality-conventions.gradle.kts\` | Unit capability alias and local-check dependency set |
| \`build-logic/src/main/kotlin/com.incode.testing-conventions.gradle.kts\` | Integration and E2E aliases and \`fastCheck\` composition |
| \`build-logic/src/main/kotlin/com.incode.contract-conventions.gradle.kts\` | Contract alias combining contract tests when present with OpenAPI validation |
| \`build-logic/src/main/kotlin/com.incode.container-conventions.gradle.kts\` | Container alias over the existing image smoke task |
| \`src/test/java/com/incode/verification/config/GradleStructureTest.java\` | Repository-owned smoke assertions for names and gate boundaries |

## Intended Task Graph

    unitCheck
      -> spotlessCheck, checkstyle*, test, jacocoTestReport,
         jacocoTestCoverageVerification

    integrationCheck -> integrationTest
    contractCheck    -> contractTest (when present), openApiValidate
    e2eCheck         -> e2eTest
    containerCheck   -> imageSmoke -> image -> bootBuildImage

    fastCheck        -> unitCheck
    qualityGate      -> existing check, openApiValidate, buildLogicCheck

Aliases are thin task groupings. They do not duplicate task actions or change
the existing full gate.

## Task 1: Add executable TestKit coverage

**Files:**

- Modify: \`build-logic/build.gradle.kts\`
- Create: \`build-logic/src/test/kotlin/com/incode/buildlogic/CapabilityTaskFunctionalTest.kt\`

### Interfaces

- Test fixture input: a temporary Gradle project with generated
  \`settings.gradle.kts\`, \`gradle/libs.versions.toml\`, and
  \`build.gradle.kts\`.
- Test fixture output: a \`BuildResult\` from \`GradleRunner\` running the actual
  service \`build-logic\` included build.
- Test property: \`buildLogicRoot\`, supplied by the build-logic test task so
  the fixture includes the current convention-plugin build.

- [ ] **Step 1: Write the failing TestKit tests.**

Create a JUnit Jupiter test class with \`@TempDir Path projectDir\`,
\`GradleRunner.create().withProjectDir(projectDir.toFile())\`, and a helper that
writes the fixture files before every run. The fixture settings must include the
absolute build-logic path in \`pluginManagement.includeBuild\`, use Maven Central
and the Gradle Plugin Portal, and create a minimal version catalog containing
the aliases already consumed by the testing, quality, and contract conventions:
\`checkstyle\`, \`google-java-format\`, \`ktlint\`, \`junit-jupiter\`,
\`junit-platform-launcher\`, the three Testcontainers libraries, and
\`redocly-cli\`.

The test class must contain these behaviors:

    @Test
    fun capabilityAliasesAreExposed() {
      val result = runFixture(
        javaTestingQualityAndContractPlugins(),
        "tasks", "--all"
      )
      assertTrue(result.output.contains("unitCheck"))
      assertTrue(result.output.contains("integrationCheck"))
      assertTrue(result.output.contains("contractCheck"))
      assertTrue(result.output.contains("e2eCheck"))
    }

    @Test
    fun fastCheckExcludesExternalCapabilities() {
      val result = runFixture(
        javaTestingQualityAndContractPlugins(),
        "fastCheck", "--dry-run"
      )
      assertTrue(result.output.contains(":unitCheck"))
      assertFalse(result.output.contains(":integrationTest"))
      assertFalse(result.output.contains(":contractTest"))
      assertFalse(result.output.contains(":e2eTest"))
      assertFalse(result.output.contains(":openApiValidate"))
      assertFalse(result.output.contains(":imageSmoke"))
    }

    @Test
    fun qualityGateRetainsExternalVerification() {
      val result = runFixture(
        javaTestingQualityAndContractPlugins(),
        "qualityGate", "--dry-run"
      )
      assertTrue(result.output.contains(":integrationTest"))
      assertTrue(result.output.contains(":contractTest"))
      assertTrue(result.output.contains(":e2eTest"))
      assertTrue(result.output.contains(":openApiValidate"))
    }

    @Test
    fun containerConventionExposesContainerCheck() {
      val result = runFixture(
        javaSpringBootAndContainerPlugins(),
        "tasks", "--all"
      )
      assertTrue(result.output.contains("containerCheck"))
    }

The two plugin-string helpers must apply the plugin IDs in dependency order:
Java, testing conventions, quality conventions, contract conventions for the
first fixture; Java, Spring Boot 4.1.1, and container conventions for the second.
The fixture must contain one Java source file and must not invoke Docker, npx,
provider services, or test containers.

- [ ] **Step 2: Configure TestKit and run RED.**

Add the existing catalog JUnit libraries and Gradle-provided TestKit dependency
to \`build-logic/build.gradle.kts\`:

    import org.gradle.api.tasks.testing.Test

    dependencies {
      testImplementation(gradleTestKit())
      testImplementation(libsCatalog.findLibrary("junit-jupiter").get())
      testRuntimeOnly(libsCatalog.findLibrary("junit-platform-launcher").get())
    }

    tasks.withType<Test>().configureEach {
      useJUnitPlatform()
      systemProperty("buildLogicRoot", rootDir.absolutePath)
    }

Run:

    ./gradlew :build-logic:test \
      --tests com.incode.buildlogic.CapabilityTaskFunctionalTest \
      --no-daemon --console=plain

Expected: the fixtures reach the convention plugins, then fail because the
capability aliases do not yet exist. A fixture configuration error is acceptable
only while fixing the fixture's catalog or included-build setup; the final RED
result must identify missing aliases.

- [ ] **Step 3: Commit the RED tests and TestKit setup.**

    git add build-logic/build.gradle.kts \
      build-logic/src/test/kotlin/com/incode/buildlogic/CapabilityTaskFunctionalTest.kt
    git commit -m "test: exercise Gradle capability tasks with TestKit"

## Task 2: Add local, integration, and E2E aliases

**Files:**

- Modify: \`build-logic/src/main/kotlin/com.incode.quality-conventions.gradle.kts\`
- Modify: \`build-logic/src/main/kotlin/com.incode.testing-conventions.gradle.kts\`
- Test: \`build-logic/src/test/kotlin/com/incode/buildlogic/CapabilityTaskFunctionalTest.kt\`

### Interfaces

- \`unitCheck\`: quality-convention task for formatting, static analysis, unit
  tests, and JaCoCo checks.
- \`integrationCheck\`: testing-convention task depending on \`integrationTest\`.
- \`e2eCheck\`: testing-convention task depending on \`e2eTest\`.
- \`fastCheck\`: existing local gate, depending on \`unitCheck\` only.

- [ ] **Step 1: Implement \`unitCheck\` in the quality convention.**

Extract the current local dependency list used by \`fastCheck\` into one task:

    tasks.register("unitCheck") {
      group = "verification"
      description = "Runs formatting, static analysis, unit tests, and coverage checks."
      dependsOn(
        "spotlessCheck",
        "checkstyleMain",
        "checkstyleTest",
        "checkstyleTestFixtures",
        "test",
        "jacocoTestReport",
        "jacocoTestCoverageVerification",
      )
    }

Do not depend on \`check\`, because \`check\` intentionally includes external
test suites.

- [ ] **Step 2: Make \`fastCheck\` delegate to \`unitCheck\`.**

Replace the duplicated local list in the testing convention with:

    tasks.register("fastCheck") {
      group = "verification"
      description = "Runs unit tests and local quality checks without external-service validation."
      dependsOn("unitCheck")
    }

Keep the task name and group unchanged. The lazy string dependency preserves
plugin application order.

- [ ] **Step 3: Add integration and E2E aliases.**

After the existing JVM test-suite registrations, add:

    tasks.register("integrationCheck") {
      group = "verification"
      description = "Runs integration tests against external service containers."
      dependsOn("integrationTest")
    }

    tasks.register("e2eCheck") {
      group = "verification"
      description = "Runs end-to-end verification tests."
      dependsOn("e2eTest")
    }

- [ ] **Step 4: Run focused GREEN verification.**

    ./gradlew :build-logic:test \
      --tests com.incode.buildlogic.CapabilityTaskFunctionalTest \
      --no-daemon --console=plain

Expected: unit, integration, E2E, and fast-check assertions pass. Contract and
container alias assertions remain RED.

- [ ] **Step 5: Commit the local/test aliases.**

    git add build-logic/src/main/kotlin/com.incode.quality-conventions.gradle.kts \
      build-logic/src/main/kotlin/com.incode.testing-conventions.gradle.kts \
      build-logic/src/test/kotlin/com/incode/buildlogic/CapabilityTaskFunctionalTest.kt
    git commit -m "build: expose local and test capability checks"

## Task 3: Add contract and container aliases

**Files:**

- Modify: \`build-logic/src/main/kotlin/com.incode.contract-conventions.gradle.kts\`
- Modify: \`build-logic/src/main/kotlin/com.incode.container-conventions.gradle.kts\`
- Modify: \`build-logic/src/test/kotlin/com/incode/buildlogic/CapabilityTaskFunctionalTest.kt\`
- Modify: \`src/test/java/com/incode/verification/config/GradleStructureTest.java\`

### Interfaces

- \`contractCheck\`: contract-convention task depending on \`openApiValidate\`
  and on \`contractTest\` only when that task exists.
- \`containerCheck\`: container-convention task depending on \`imageSmoke\`.
- \`GradleStructureTest\`: smoke assertions for alias names and local-only
  \`fastCheck\`.

- [ ] **Step 1: Add RED source-structure assertions.**

Extend \`GradleStructureTest\` to assert that the loaded testing, quality,
contract, and container convention source includes \`integrationCheck\`,
\`e2eCheck\`, \`unitCheck\`, \`contractCheck\`, and \`containerCheck\`.
Also inspect the \`fastCheck\` substring and assert it contains \`unitCheck\` but
not \`integrationTest\`, \`contractTest\`, \`e2eTest\`, \`openApiValidate\`, or
\`imageSmoke\`.

Run:

    ./gradlew test --tests com.incode.verification.config.GradleStructureTest \
      --no-daemon --console=plain

Expected: RED because the new aliases have not been added.

- [ ] **Step 2: Implement \`contractCheck\`.**

Register it after \`openApiValidate\`:

    val contractCheck = tasks.register("contractCheck") {
      group = "verification"
      description = "Runs API contract tests and OpenAPI validation."
      dependsOn("openApiValidate")
    }

    tasks.matching { task -> task.name == "contractTest" }.all { contractTestTask ->
      contractCheck.configure { dependsOn(contractTestTask) }
    }

The live task collection keeps the contract-test dependency conditional when the
contract convention is applied without the testing convention.

- [ ] **Step 3: Implement \`containerCheck\`.**

Register it after the existing \`imageSmoke\` task:

    tasks.register("containerCheck") {
      group = "verification"
      description = "Builds the configured image and runs the bounded image smoke check."
      dependsOn("imageSmoke")
    }

Do not change digest validation, pull policy, cache volumes, native-image
variables, or image smoke cleanup.

- [ ] **Step 4: Run focused GREEN verification.**

    ./gradlew :build-logic:test \
      --tests com.incode.buildlogic.CapabilityTaskFunctionalTest \
      --no-daemon --console=plain
    ./gradlew test --tests com.incode.verification.config.GradleStructureTest \
      --no-daemon --console=plain

Expected: all aliases and gate-boundary assertions pass. No Docker, npx,
provider, or external integration process executes.

- [ ] **Step 5: Commit the external aliases.**

    git add build-logic/src/main/kotlin/com.incode.contract-conventions.gradle.kts \
      build-logic/src/main/kotlin/com.incode.container-conventions.gradle.kts \
      build-logic/src/test/kotlin/com/incode/buildlogic/CapabilityTaskFunctionalTest.kt \
      src/test/java/com/incode/verification/config/GradleStructureTest.java
    git commit -m "build: expose contract and container capability checks"

## Task 4: Refactor and verify the complete build

**Files:**

- Modify: \`src/test/java/com/incode/verification/config/GradleStructureTest.java\`
  only if focused verification exposes a missing smoke invariant.
- Modify: design spec only if a confirmed behavior difference requires a design
  correction.

- [ ] **Step 1: Review for duplication and lazy configuration.**

Confirm each alias has one owner, no alias duplicates task actions, optional
dependencies use lazy task names or live task collections, and
\`qualityGate\`/ \`verifyFinalGates\` retain their existing definitions.

- [ ] **Step 2: Run build-logic checks.**

    ./gradlew :build-logic:check --no-daemon --console=plain

Expected: formatting, detekt, build-logic compilation, TestKit tests, and plugin
validation pass.

- [ ] **Step 3: Run the service local gate.**

    ./gradlew fastCheck --no-daemon --console=plain

Expected: only unit and local quality work executes; external capability tasks
do not appear in the execution output.

- [ ] **Step 4: Inspect each focused task graph.**

    ./gradlew unitCheck --dry-run --no-daemon --console=plain
    ./gradlew integrationCheck --dry-run --no-daemon --console=plain
    ./gradlew contractCheck --dry-run --no-daemon --console=plain
    ./gradlew e2eCheck --dry-run --no-daemon --console=plain
    ./gradlew containerCheck --dry-run --no-daemon --console=plain \
      -PpaketoBuilderImage=paketobuildpacks/builder-jammy-base@sha256:0000000000000000000000000000000000000000000000000000000000000000 \
      -PpaketoRunImage=paketobuildpacks/run-jammy-base@sha256:0000000000000000000000000000000000000000000000000000000000000000

Expected: each alias resolves to its intended existing tasks. The container
dry-run must not start Docker.

- [ ] **Step 5: Run the complete service gate when external services are available.**

    ./gradlew qualityGate --no-daemon --console=plain

Expected: existing integration, contract, E2E, OpenAPI, coverage, and build-logic
checks remain in the full gate. If Docker, PostgreSQL, Redis, or provider
services are unavailable, record the exact blocked task and retain focused test
results.

- [ ] **Step 6: Verify scope and commit final requested changes.**

    git diff --check
    git status --short
    git diff --stat HEAD~3..HEAD

Confirm unrelated user changes were not staged or modified. Stage only final
requested test/documentation changes:

    git add src/test/java/com/incode/verification/config/GradleStructureTest.java
    git commit -m "test: verify Gradle capability gate boundaries"

## Definition of Done

- [ ] TestKit executes real fixture builds against the included convention plugins.
- [ ] All five capability aliases resolve to intended existing task behavior.
- [ ] \`fastCheck\` remains local-only.
- [ ] \`qualityGate\` retains its full existing verification behavior.
- [ ] Build-logic and focused service checks pass.
- [ ] Full \`qualityGate\` passes, or external blockers are documented with task output.
- [ ] No custom capability DSL, business modules, or runtime dependencies were added.
- [ ] Existing unrelated working-tree changes remain untouched.

