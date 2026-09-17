package com.incode.build;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class BuildConfigurationTest {
  @Test
  void exposesRequiredBuildTasks() throws Exception {
    Result result = run("tasks", "--all");
    assertEquals(0, result.exitCode());
    String o = result.output();
    assertTrue(o.contains("bootBuildImage"));
    assertTrue(o.contains("containerCheck"));
    assertTrue(o.contains("integrationTest"));
    assertTrue(o.contains("contractTest"));
    assertTrue(o.contains("e2eTest"));
    assertTrue(o.contains("qualityGate"));
    assertTrue(o.contains("verifyFinalGates"));
    assertFalse(o.contains("Builds the JVM or native image with Paketo"));
  }

  @Test
  void rejectsUnknownImageVariant() throws Exception {
    Result result = run("-PimageVariant=invalid");
    assertEquals(1, result.exitCode());
    assertTrue(result.output().contains("imageVariant must be one of: jvm, native"));
  }

  @Test
  void rejectsAnUnsupportedImagePlatform() throws Exception {
    Result result = run("-PimagePlatform=darwin/arm64");
    assertEquals(1, result.exitCode());
    assertTrue(
        result.output().contains("imagePlatform must be linux/amd64 or linux/arm64"),
        result.output());
  }

  private Result run(String... a) throws Exception {
    String[] c = new String[a.length + 1];
    c[0] = "./gradlew";
    System.arraycopy(a, 0, c, 1, a.length);
    Process p =
        new ProcessBuilder(c).directory(Path.of(".").toFile()).redirectErrorStream(true).start();
    String o = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    if (!p.waitFor(2, TimeUnit.MINUTES)) {
      p.destroyForcibly();
      fail("Gradle subprocess timed out");
    }
    return new Result(p.exitValue(), o);
  }

  private record Result(int exitCode, String output) {}
}
