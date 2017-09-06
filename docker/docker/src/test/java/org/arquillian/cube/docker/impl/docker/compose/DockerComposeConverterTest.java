package org.arquillian.cube.docker.impl.docker.compose;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;

import org.arquillian.cube.docker.impl.client.config.CubeContainer;
import org.arquillian.cube.docker.impl.client.config.DockerCompositions;
import org.arquillian.cube.docker.impl.client.config.Link;
import org.arquillian.cube.docker.impl.client.config.Network;
import org.arquillian.cube.docker.impl.client.config.PortBinding;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class DockerComposeConverterTest {

  @Test
  public void shouldTransformSimpleDockerComposeFormat() throws URISyntaxException, IOException {

    URI simpleDockerCompose = DockerComposeConverterTest.class.getResource("/simple-docker-compose.yml").toURI();
    DockerComposeConverter dockerComposeConverter = DockerComposeConverter.create(Paths.get(simpleDockerCompose));

    DockerCompositions convert = dockerComposeConverter.convert();
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

    DockerCompositions convert = dockerComposeConverter.convert();
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

    DockerCompositions convert = dockerComposeConverter.convert();
    CubeContainer webapp = convert.get("webapp2");
    assertThat(webapp.getImage(), is(notNullValue()));
    final String image = webapp.getImage().toImageRef();
    assertThat(image, is(expectedImageName));
  }

  @Test
  public void shouldExtendDockerCompose() throws URISyntaxException, IOException {
    URI readEnvsDockerCompose = DockerComposeConverterTest.class.getResource("/extends-docker-compose.yml").toURI();
    DockerComposeConverter dockerComposeConverter = DockerComposeConverter.create(Paths.get(readEnvsDockerCompose));

    DockerCompositions convert = dockerComposeConverter.convert();
    CubeContainer webapp = convert.get("web");
    assertThat(webapp.getEnv(), is(notNullValue()));
    Collection<String> env = webapp.getEnv();
    assertThat(env, containsInAnyOrder("REDIS_HOST=redis-production.example.com"));
    assertThat(webapp.getPortBindings(), is(notNullValue()));
    Collection<PortBinding> ports = webapp.getPortBindings();
    assertThat(ports, containsInAnyOrder(PortBinding.valueOf("8080->8080")));
  }

  @Test
  public void shouldExtendDockerComposeWithEnvResolution() throws URISyntaxException, IOException {

    String oldValue = System.getProperty("ports");
    System.setProperty("ports", "9090:8080");

    try {
      URI readEnvsDockerCompose = DockerComposeConverterTest.class.getResource("/extends-docker-compose-env.yml").toURI();
      DockerComposeConverter dockerComposeConverter = DockerComposeConverter.create(Paths.get(readEnvsDockerCompose));

      DockerCompositions convert = dockerComposeConverter.convert();
      CubeContainer webapp = convert.get("web");
      assertThat(webapp.getEnv(), is(notNullValue()));
      Collection<String> env = webapp.getEnv();
      assertThat(env, containsInAnyOrder("REDIS_HOST=redis-production.example.com"));
      assertThat(webapp.getPortBindings(), is(notNullValue()));
      Collection<PortBinding> ports = webapp.getPortBindings();
      assertThat(ports, containsInAnyOrder(PortBinding.valueOf("9090->8080")));
    } finally {
      System.clearProperty("ports");
      if (oldValue != null)
        System.setProperty("ports", oldValue);
    }
  }

  @Test
  public void shouldTransformSimpleDockerComposeV2FormatWithNetworksByDefaultDriver() throws URISyntaxException, IOException {

    URI simpleDockerCompose = DockerComposeConverterTest.class.getResource("/simple-docker-compose-networks-v2.yml").toURI();
    DockerComposeConverter dockerComposeConverter = DockerComposeConverter.create(Paths.get(simpleDockerCompose));

    DockerCompositions convert = dockerComposeConverter.convert();
    CubeContainer webapp = convert.getContainers().get("webapp");
    assertThat(webapp.getBuildImage(), is(notNullValue()));
    assertThat(webapp.getPortBindings(), is(notNullValue()));
    Collection<PortBinding> ports = webapp.getPortBindings();
    assertThat(ports, containsInAnyOrder(PortBinding.valueOf("8000->8000")));
    assertThat(webapp.getDevices(), is(notNullValue()));
    assertThat(webapp.getVolumes(), is(notNullValue()));
    Collection<String> webappVolumes = (Collection<String>) webapp.getVolumes();
    assertThat(webappVolumes, containsInAnyOrder("/data"));

    CubeContainer webapp2 = convert.getContainers().get("webapp2");
    assertThat(webapp2.getImage(), is(notNullValue()));
    assertThat(webapp2.getPortBindings(), is(notNullValue()));
    assertThat(webapp2.getLinks(), is(notNullValue()));
    Collection<Link> links = webapp2.getLinks();
    assertThat(links, containsInAnyOrder(Link.valueOf("webapp:webapp")));
    assertThat(webapp2.getEnv(), is(notNullValue()));
    Collection<String> env = webapp2.getEnv();
    assertThat(env, containsInAnyOrder("RACK_ENV=development"));

    Map<String, Network> networks = convert.getNetworks();
    Network network = networks.get("front-tier");
    assertThat(network.getDriver(), is("bridge"));

    Network network2 = networks.get("back-tier");
    assertThat(network2.getDriver(), is("bridge"));
  }

  @Test
  public void shouldTransformSimpleDockerComposeV2FormatWithNetworks() throws URISyntaxException, IOException {

    URI simpleDockerCompose = DockerComposeConverterTest.class.getResource("/simple-docker-compose-v2.yml").toURI();
    DockerComposeConverter dockerComposeConverter = DockerComposeConverter.create(Paths.get(simpleDockerCompose));

    DockerCompositions convert = dockerComposeConverter.convert();
    CubeContainer webapp = convert.getContainers().get("webapp");
    assertThat(webapp.getBuildImage(), is(notNullValue()));
    assertThat(webapp.getPortBindings(), is(notNullValue()));
    Collection<PortBinding> ports = webapp.getPortBindings();
    assertThat(ports, containsInAnyOrder(PortBinding.valueOf("8000->8000")));
    assertThat(webapp.getDevices(), is(notNullValue()));
    assertThat(webapp.getVolumes(), is(notNullValue()));
    Collection<String> webappVolumes = (Collection<String>) webapp.getVolumes();
    assertThat(webappVolumes, containsInAnyOrder("/data"));

    CubeContainer webapp2 = convert.getContainers().get("webapp2");
    assertThat(webapp2.getImage(), is(notNullValue()));
    assertThat(webapp2.getPortBindings(), is(notNullValue()));
    assertThat(webapp2.getLinks(), is(notNullValue()));
    Collection<Link> links = webapp2.getLinks();
    assertThat(links, containsInAnyOrder(Link.valueOf("webapp:webapp")));
    assertThat(webapp2.getEnv(), is(notNullValue()));
    Collection<String> env = webapp2.getEnv();
    assertThat(env, containsInAnyOrder("RACK_ENV=development"));

    Map<String, Network> networks = convert.getNetworks();
    Network network = networks.get("front-tier");
    assertThat(network.getDriver(), is("bridge"));

    Network network2 = networks.get("back-tier");
    assertThat(network2.getDriver(), is("bridge"));
  }

  @Test
  public void shouldTransformSimpleDockerComposeV2FormatMultipleNetworks() throws URISyntaxException {

    URI simpleDockerCompose = DockerComposeConverterTest.class.getResource("/myapp/docker-compose-multiple-networks.yml").toURI();
    DockerComposeConverter dockerComposeConverter = DockerComposeConverter.create(Paths.get(simpleDockerCompose));

    DockerCompositions convert = dockerComposeConverter.convert();
    CubeContainer webapp = convert.getContainers().get("pingpong");
    assertThat(webapp, is(notNullValue()));
    assertThat(webapp.getNetworkMode()).isEqualTo("front");
    assertThat(webapp.getNetworks()).contains("front", "back");

    Map<String, Network> networks = convert.getNetworks();
    assertThat(networks).hasSize(2);
    assertThat(networks).containsOnlyKeys("front", "back");
  }

  @Test
  public void shouldTransformSimpleDockerComposeV2FormatNetworkByDefault() throws URISyntaxException {

    URI simpleDockerCompose = DockerComposeConverterTest.class.getResource("/myapp/simple-docker-compose-v2.yml").toURI();
    DockerComposeConverter dockerComposeConverter = DockerComposeConverter.create(Paths.get(simpleDockerCompose));

    DockerCompositions convert = dockerComposeConverter.convert();
    CubeContainer webapp = convert.getContainers().get("webapp");
    assertThat(webapp, is(notNullValue()));
    assertThat(webapp.getNetworkMode()).endsWith("_default");

    Map<String, Network> networks = convert.getNetworks();
    assertThat(networks).hasSize(1);
    assertThat(networks.keySet().iterator().next()).endsWith("_default");
  }

  @Test
  public void shouldBuildImageFromContextProperty() throws URISyntaxException {
    URI simpleDockerCompose = DockerComposeConverterTest.class.getResource("/simple-docker-compose-build-with-context-dir.yml").toURI();
    DockerComposeConverter dockerComposeConverter = DockerComposeConverter.create(Paths.get(simpleDockerCompose));

    DockerCompositions convert = dockerComposeConverter.convert();
    CubeContainer webapp = convert.get("webapp");
    assertThat(webapp.getBuildImage(), is(notNullValue()));
  }

  @Test
  public void shouldBuildImageFromBuildProperty() throws URISyntaxException {
    URI simpleDockerCompose = DockerComposeConverterTest.class.getResource("/simple-docker-compose-build-with-dockerfile.yml").toURI();
    DockerComposeConverter dockerComposeConverter = DockerComposeConverter.create(Paths.get(simpleDockerCompose));

    DockerCompositions convert = dockerComposeConverter.convert();
    CubeContainer webapp = convert.get("webapp");
    assertThat(webapp.getBuildImage(), is(notNullValue()));
  }

  @Test
  public void shouldTransformSimpleDockerComposeV2FormatNetworkAliases() throws URISyntaxException {
    URI simpleDockerCompose = DockerComposeConverterTest.class.getResource("/simple-docker-compose-network-aliases-v2.yml").toURI();
    DockerComposeConverter dockerComposeConverter = DockerComposeConverter.create(Paths.get(simpleDockerCompose));

    DockerCompositions convert = dockerComposeConverter.convert();
    CubeContainer webapp = convert.get("webapp");
    assertThat(webapp, is(notNullValue()));
    assertThat(webapp.getNetworks()).contains("front-tier");
    assertThat(webapp.getAliases()).containsOnly("foo", "bar");
  }

}
