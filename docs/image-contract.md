# Image contract

`image` and `bootBuildImage` require two immutable Paketo references:

- `paketoBuilderImage`
- `paketoRunImage`

Each value must end in `@sha256:` followed by exactly 64 hexadecimal characters.
The repository deliberately does not select or invent digest values. Supply the
approved references in `gradle.properties` (which is local configuration) or as
`-P` properties. A copyable template is in `gradle.properties.example`.

For the complete local setup, use one of the repository's `mise` commands from
the workspace root:

```sh
mise run setup-jvm
# or
mise run setup-native
```

Both commands run the provider checks, build the provider and service local
images, start the single-node Compose stack, and wait for the backend health
endpoint. They create `.env` from `.env.example` only when `.env` is absent.
They still require approved digest-pinned Paketo references in the local
`gradle.properties`; the setup does not invent or overwrite those values.

## Image-build memory guidance

The setup runner checks the active Docker daemon before starting a build. These
are repository operational recommendations, not hard Paketo platform minimums:

| Image path | Docker memory | Reason |
|---|---:|---|
| JVM | 4 GiB recommended | 2 GiB is a constrained lower-bound attempt and may fail from Gradle/Paketo overhead. |
| Native | 12 GiB recommended | The native build includes the GraalVM/native-image toolchain and native compilation. |

See the [Paketo Java Native Image Buildpack reference](https://paketo.io/docs/reference/java-native-image-reference/)
for the buildpack-provided native-image toolchain contract. The runner does
not stop or resize an existing Docker Desktop or Colima runtime.

The `composeDigestCheck` Gradle task applies the same immutable-reference rule
to every image input used by the workspace Compose file. Supply the Compose
image variables through the environment or `-P` properties before invoking it.

The default is the JVM image. Native is explicit with
`-PimageVariant=native -PnativeOptimization=b`; native builds receive exactly
`-Ob`. Other optimization values and unknown variants are rejected during task
configuration.

For a local executable, `nativeCompile` selects the Java version declared by
the project and requests a native-image-capable toolchain. Gradle provisions
that JDK through the Foojay resolver when it is not installed locally, so the
Temurin JDK used to run ordinary Gradle tasks does not need to contain
`native-image`:

```sh
./gradlew nativeCompile --no-daemon --console=plain
```

For a container image, use the existing Paketo path. Its native builder owns
the GraalVM/native-image toolchain inside the build container:

```sh
./gradlew image \
  -PimageVariant=native \
  -PnativeOptimization=b \
  -PimageName=company-check-service:local
```

The two paths produce the same native-image application, but local
`nativeCompile` produces an executable while `image` produces the OCI image
used by Compose and the distributed Locust runner.

Publishing is also Gradle-owned. Set `-PpublishImage=true`, an explicit
`-PimageName`, and `-PimagePlatform=linux/amd64` or `linux/arm64`. Release
workflows provide the approved Paketo references through repository variables;
they do not build through a separate Dockerfile path.

The image task emits OCI metadata labels and keeps Paketo's non-root runtime
contract. `imageSmoke` is a bounded Gradle-owned host-side smoke check: it uses
the locally built image, runs the process as its declared non-root user, checks
the container exits within the deadline, and never requires Docker inside the
image. Configure the image with `-PimageName=...` and the deadline with
`-PimageSmokeTimeoutSeconds=...`.
