package j2html.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.nio.file.Path;
import java.nio.file.Paths;

@Mojo(name = "generate-tags", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class TagGeneratorMojo extends AbstractMojo {

  @Parameter(defaultValue = "${project.build.directory}")
  private String projectBuildDir;

  @Parameter(defaultValue = "${project.build.resources.resource.directory}")
  private String projectResourcesDir;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    Path tagClassesJson = Paths.get("src", "main", "resources", "tagClasses.json");
    getLog().info("Build dir is: " + projectBuildDir);
    getLog().info("manual: " + tagClassesJson);
    getLog().info("Build resources is: " + projectResourcesDir);

    new TagCreatorCodeGeneratorFromJson().execute(tagClassesJson, Paths.get(projectBuildDir, "generated-sources", "j2html"));
  }
}
