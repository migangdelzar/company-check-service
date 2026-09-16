package com.incode.build;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
class BuildConfigurationTest {
  @Test void exposesRequiredBuildTasks() throws Exception {
    String o = run("tasks", "--all");
    assertTrue(o.contains("image")); assertTrue(o.contains("integrationTest"));
    assertTrue(o.contains("contractTest")); assertTrue(o.contains("e2eTest"));
  }
  @Test void rejectsUnknownImageVariant() throws Exception {
    assertTrue(run("image", "-PimageVariant=invalid")
        .contains("imageVariant must be one of: jvm, native"));
  }
  private String run(String... a) throws Exception {
    String[] c = new String[a.length + 1]; c[0] = "./gradlew";
    System.arraycopy(a, 0, c, 1, a.length);
    Process p = new ProcessBuilder(c).directory(Path.of(".").toFile())
        .redirectErrorStream(true).start();
    String o = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    p.waitFor(2, TimeUnit.MINUTES); return o;
  }
}
