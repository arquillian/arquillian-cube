package org.arquillian.cube.docker.impl.docker.compose;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Collection;

import org.arquillian.cube.docker.impl.client.config.CubeContainer;
import org.arquillian.cube.docker.impl.client.config.CubeContainers;
import org.arquillian.cube.docker.impl.client.config.Link;
import org.arquillian.cube.docker.impl.client.config.PortBinding;
import org.junit.Test;

public class DockerComposeConverterTest {

  @Test
  public void shouldTransformSimpleDockerComposeFormat() throws URISyntaxException, IOException {

    URI simpleDockerCompose = DockerComposeConverterTest.class.getResource("/simple-docker-compose.yml").toURI();
    DockerComposeConverter dockerComposeConverter = DockerComposeConverter.create(Paths.get(simpleDockerCompose));

    CubeContainers convert = dockerComposeConverter.convert();
    CubeContainer webapp = convert.get("webapp");
    assertThat(webapp.getBuildImage(), is(notNullValue()));
    assertThat(webapp.getPortBindings(), is(notNullValue()));
    Collection<PortBinding> ports = webapp.getPortBindings();
    assertThat(ports, containsInAnyOrder(PortBinding.valueOf("8000->8000")));
    assertThat(webapp.getDevices(), is(notNullValue()));
    assertThat(webapp.getVolumes(), is(notNullValue()));
    Collection<String> volumes = (Collection<String>) webapp.getVolumes();
    assertThat(volumes, containsInAnyOrder("/data"));

    CubeContainer webapp2 = convert.get("webapp2");
    assertThat(webapp2.getImage(), is(notNullValue()));
    assertThat(webapp2.getPortBindings(), is(notNullValue()));
    assertThat(webapp2.getLinks(), is(notNullValue()));
    Collection<Link> links = webapp2.getLinks();
    assertThat(links, containsInAnyOrder(Link.valueOf("webapp:webapp")));
    assertThat(webapp2.getEnv(), is(notNullValue()));
    Collection<String> env = webapp2.getEnv();
    assertThat(env, containsInAnyOrder("RACK_ENV=development"));
  }

  @Test
  public void shouldReadEnvironmentVarsFromFile() throws URISyntaxException, IOException {
    URI readEnvsDockerCompose = DockerComposeConverterTest.class.getResource("/read-envs-docker-compose.yml").toURI();
    DockerComposeConverter dockerComposeConverter = DockerComposeConverter.create(Paths.get(readEnvsDockerCompose));

    CubeContainers convert = dockerComposeConverter.convert();
    CubeContainer webapp = convert.get("webapp");
    assertThat(webapp.getEnv(), is(notNullValue()));
    Collection<String> env = webapp.getEnv();
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

    CubeContainers convert = dockerComposeConverter.convert();
    CubeContainer webapp = convert.get("webapp2");
    assertThat(webapp.getImage(), is(notNullValue()));
    final String image = webapp.getImage().toImageRef();
    assertThat(image, is(expectedImageName));
  }

  @Test
  public void shouldExtendDockerCompose() throws URISyntaxException, IOException {
    URI readEnvsDockerCompose = DockerComposeConverterTest.class.getResource("/extends-docker-compose.yml").toURI();
    DockerComposeConverter dockerComposeConverter = DockerComposeConverter.create(Paths.get(readEnvsDockerCompose));

    CubeContainers convert = dockerComposeConverter.convert();
    CubeContainer webapp = convert.get("web");
    assertThat(webapp.getEnv(), is(notNullValue()));
    Collection<String> env = webapp.getEnv();
    assertThat(env, containsInAnyOrder("REDIS_HOST=redis-production.example.com"));
    assertThat(webapp.getPortBindings(), is(notNullValue()));
    Collection<PortBinding> ports = webapp.getPortBindings();
    assertThat(ports, containsInAnyOrder(PortBinding.valueOf("8080->8080")));
  }
}
