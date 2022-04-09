package com.github.maracas.forges.build.maven;

import com.github.maracas.forges.build.BuildConfig;
import com.github.maracas.forges.build.BuildException;
import com.github.maracas.forges.build.Builder;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class MavenBuilderTest {
  final Path testProject = Paths.get("src/test/resources/maven-project/");
  final Path invalidProject = Paths.get("src/test/resources/");
  final Path validTarget = testProject.resolve("target/");
  final Path testProjectError = Paths.get("src/test/resources/maven-project-error/");

  @BeforeEach
  void setUp() throws IOException {
    FileUtils.deleteDirectory(validTarget.toFile());
  }

  @Test
  void build_validPom_default() {
    Builder builder = new MavenBuilder(new BuildConfig(testProject));
    builder.build();
    assertTrue(builder.locateJar().isPresent());
  }

  @Test
  void build_validPom_withGoal() {
    BuildConfig configWithGoal = new BuildConfig(testProject);
    configWithGoal.addGoal("clean");
    Builder builder = new MavenBuilder(configWithGoal);
    builder.build();
    assertFalse(builder.locateJar().isPresent());
  }

  @Test
  void build_validPom_withProperty() {
    BuildConfig configWithProperty = new BuildConfig(testProject);
    configWithProperty.setProperty("maven.test.skip", "false");
    Builder builder = new MavenBuilder(configWithProperty);
    assertThrows(BuildException.class, builder::build);
  }

  @Test
  void build_compileError() {
    Builder builder = new MavenBuilder(new BuildConfig(testProjectError));
    assertThrows(BuildException.class, builder::build);
  }

  @Test
  void build_invalidProject() {
    Builder builder = new MavenBuilder(new BuildConfig(invalidProject));
    assertThrows(BuildException.class, builder::build);
  }

  @Test
  void build_invalidGoal() {
    BuildConfig configWithInvalidGoals = new BuildConfig(testProject);
    configWithInvalidGoals.addGoal("nope");
    Builder builder = new MavenBuilder(configWithInvalidGoals);
    assertThrows(BuildException.class, builder::build);
  }
}