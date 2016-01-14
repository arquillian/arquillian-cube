package org.arquillian.cube.docker.impl.docker.compose;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;

import org.junit.Test;

public class DockerComposeConverterTest {

  @Test
  public void shouldTransformSimpleDockerComposeFormat() throws URISyntaxException, IOException {

    URI simpleDockerCompose = DockerComposeConverterTest.class.getResource("/simple-docker-compose.yml").toURI();
    DockerComposeConverter dockerComposeConverter = DockerComposeConverter.create(Paths.get(simpleDockerCompose));

    Map<String, Object> convert = dockerComposeConverter.convert();
    Map<String, Object> webapp = (Map<String, Object>) convert.get("webapp");
    assertThat(webapp, hasKey("buildImage"));
    assertThat(webapp, hasKey("portBindings"));
    Collection<String> ports = (Collection<String>) webapp.get("portBindings");
    assertThat(ports, containsInAnyOrder("8000->8000"));
    assertThat(webapp, hasKey("devices"));
    assertThat(webapp, hasKey("volumes"));
    Collection<String> volumes = (Collection<String>) webapp.get("volumes");
    assertThat(volumes, containsInAnyOrder("/data"));

    Map<String, Object> webapp2 = (Map<String, Object>) convert.get("webapp2");
    assertThat(webapp2, hasKey("image"));
    assertThat(webapp2, hasKey("portBindings"));
    assertThat(webapp2, hasKey("links"));
    Collection<String> links = (Collection<String>) webapp2.get("links");
    assertThat(links, containsInAnyOrder("webapp:webapp"));
    assertThat(webapp2, hasKey("env"));
    Collection<String> env = (Collection<String>) webapp2.get("env");
    assertThat(env, containsInAnyOrder("RACK_ENV=development"));
  }

  @Test
  public void shouldReadEnvironmentVarsFromFile() throws URISyntaxException, IOException {
    URI readEnvsDockerCompose = DockerComposeConverterTest.class.getResource("/read-envs-docker-compose.yml").toURI();
    DockerComposeConverter dockerComposeConverter = DockerComposeConverter.create(Paths.get(readEnvsDockerCompose));

    Map<String, Object> convert = dockerComposeConverter.convert();
    Map<String, Object> webapp = (Map<String, Object>) convert.get("webapp");
    assertThat(webapp, hasKey("env"));
    Collection<String> env = (Collection<String>) webapp.get("env");
    assertThat(env, containsInAnyOrder("RACK_ENV=development"));
  }

  @Test
  public void shouldResolveEnvironmentVars() throws URISyntaxException, IOException {
    testResolvePlaceholders("/simple-cube-var.yml", "MyImageName");
  }

  @Test
  public void shouldResolveSystemEnvironmentVars() throws URISyntaxException, IOException {
    testResolvePlaceholders("/simple-cube-system-var.yml", "TestImageName");
  }

  private void testResolvePlaceholders(String dockerComposeFile, String expectedImageName) throws URISyntaxException, IOException {
    URI readEnvsDockerCompose = DockerComposeConverterTest.class.getResource(dockerComposeFile).toURI();
    DockerComposeConverter dockerComposeConverter = DockerComposeConverter.create(Paths.get(readEnvsDockerCompose));

    Map<String, Object> convert = dockerComposeConverter.convert();
    Map<String, Object> webapp = (Map<String, Object>) convert.get("webapp2");
    assertThat(webapp, hasKey("image"));
    final String image = (String)webapp.get("image");
    assertThat(image, is(expectedImageName));
  }

  @Test
  public void shouldExtendDockerCompose() throws URISyntaxException, IOException {
    URI readEnvsDockerCompose = DockerComposeConverterTest.class.getResource("/extends-docker-compose.yml").toURI();
    DockerComposeConverter dockerComposeConverter = DockerComposeConverter.create(Paths.get(readEnvsDockerCompose));

    Map<String, Object> convert = dockerComposeConverter.convert();
    Map<String, Object> webapp = (Map<String, Object>) convert.get("web");
    assertThat(webapp, hasKey("env"));
    Collection<String> env = (Collection<String>) webapp.get("env");
    assertThat(env, containsInAnyOrder("REDIS_HOST=redis-production.example.com"));
    assertThat(webapp, hasKey("portBindings"));
    Collection<String> ports = (Collection<String>) webapp.get("portBindings");
    assertThat(ports, containsInAnyOrder("8080->8080"));
  }
}
