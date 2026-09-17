# Image contract

`image` and `bootBuildImage` require two immutable Paketo references:

- `paketoBuilderImage`
- `paketoRunImage`

Each value must end in `@sha256:` followed by exactly 64 hexadecimal characters.
The repository deliberately does not select or invent digest values. Supply the
approved references in `gradle.properties` (which is local configuration) or as
`-P` properties. A copyable template is in `gradle.properties.example`.

The `composeDigestCheck` Gradle task applies the same immutable-reference rule
to every image input used by the workspace Compose file. Supply the Compose
image variables through the environment or `-P` properties before invoking it.

The default is the JVM image. Native is explicit with
`-PimageVariant=native -PnativeOptimization=b`; native builds receive exactly
`-Ob`. Other optimization values and unknown variants are rejected during task
configuration.

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
