# Gradle Capability Checks Design

| Field | Detail |
|---|---|
| Date | 2026-09-17 |
| Scope | `company-check-service` Gradle build |
| Status | Draft — user review requested |

## Summary

Improve the existing single-project Gradle build by making its build capabilities
explicit and executable: local unit checks, integration checks, contract checks,
and container checks. Add functional tests for the included `build-logic` using
Gradle TestKit so convention-plugin behavior is verified by real Gradle builds,
not only by source-text assertions.

The service remains one deployable Gradle project. No business-capability
subprojects, Gradle feature variants, or custom capability DSL will be added.

## Goals

- Make capability-specific commands discoverable through standard Gradle tasks.
- Preserve the current meaning of `fastCheck` and `qualityGate`.
- Fail functional build-logic tests when plugins stop applying their expected
  tasks, dependencies, or lifecycle relationships.
- Keep all dependency versions in the existing version catalog.
- Preserve current dependency locking, configuration cache, build cache,
  OpenAPI, container, native-image, and external-service behavior.

## Non-goals

- Splitting Java production code into Gradle modules.
- Adding a custom `capabilities {}` extension.
- Changing HTTP, persistence, provider, or runtime behavior.
- Making container or external-service checks part of `fastCheck`.

## Design

### Capability task model

Use convention plugins and ordinary Gradle task composition. Each alias is
registered only where its underlying capability exists:

| Capability | Task | Responsibilities |
|---|---|---|
| Unit | `unitCheck` | Formatting, static analysis, unit tests, and JaCoCo checks already used by `fastCheck` |
| Integration | `integrationCheck` | Existing `integrationTest` suite |
| Contract | `contractCheck` | Existing `contractTest` suite and OpenAPI validation where available |
| E2E | `e2eCheck` | Existing `e2eTest` suite |
| Container | `containerCheck` | Existing image build/smoke tasks; no execution from `fastCheck` |

The aliases are thin task groupings, not new execution logic. `fastCheck`
continues to execute only local checks. `qualityGate` continues to execute the
complete existing service gate. New aliases make the task graph easier to
inspect and allow focused local iteration, for example:

```text
./gradlew unitCheck
./gradlew integrationCheck
./gradlew contractCheck
./gradlew containerCheck
./gradlew qualityGate
```

The implementation will use lazy task providers and conditional registration
so projects that do not apply a related convention plugin do not receive broken
task dependencies.

### Build-logic functional tests

Add Gradle TestKit support to `build-logic` and create isolated fixture builds
for the convention plugins. Functional tests will execute a real Gradle runner
and assert:

- the testing and quality conventions apply successfully together;
- expected capability aliases exist;
- `unitCheck` does not depend on external integration, contract, E2E, OpenAPI,
  or container work;
- full gate aliases retain their expected task relationships;
- invalid or missing plugin prerequisites fail with a useful configuration
  message where the convention requires one.

Fixtures will use a minimal local version catalog and Java source/test inputs.
They will not invoke Docker, external providers, `npx`, or image builds. The
existing source-based `GradleStructureTest` remains temporarily as a smoke test
for repository layout; TestKit becomes the source of truth for build behavior.

### Dependency and version policy

Use the existing `build-logic` included build and version catalog. Add only the
Gradle-provided TestKit dependency required to run plugin functional tests; no
new runtime or application dependency is planned. Keep dependency locking and
strict dependency verification enabled.

## Verification strategy

1. Add failing TestKit tests for the expected task graph.
2. Add the minimum convention-plugin task wiring and TestKit setup.
3. Run focused build-logic tests and the service `fastCheck`.
4. Run the complete `qualityGate` when the environment supports its external
   services and container checks.
5. Confirm the working tree contains no changes outside the requested build
   logic, tests, and documentation.

Relevant references:

- [Gradle convention plugins](https://docs.gradle.org/current/userguide/implementing_gradle_plugins_convention.html)
- [Gradle TestKit](https://docs.gradle.org/current/userguide/testing_gradle_plugins.html)
- [JVM Test Suite plugin](https://docs.gradle.org/current/userguide/jvm_test_suite_plugin.html)

## Risks and trade-offs

- TestKit adds build-logic test setup and makes verification slower than static
  text assertions, but it tests the actual Gradle behavior and catches plugin
  regressions at the correct boundary.
- Alias tasks add a small amount of build vocabulary, but they do not duplicate
  checks or alter existing gates.
- Some plugin behavior may require a fixture-specific version catalog; the
  fixture will keep that catalog minimal and explicit.

## Acceptance criteria

- `build-logic` has executable TestKit coverage for the convention-plugin task
  graph.
- Capability aliases are available only when their underlying tasks exist.
- `fastCheck` remains free of integration, contract, E2E, OpenAPI, and container
  execution.
- `qualityGate` retains its current full-gate dependencies.
- Existing service tests and Gradle checks pass, subject to documented external
  environment requirements.
- No custom capability DSL or business Gradle modules are introduced.
