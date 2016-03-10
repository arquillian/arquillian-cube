package org.arquillian.cube.docker.impl.client;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.arquillian.cube.docker.impl.client.config.CubeContainer;
import org.arquillian.cube.docker.impl.client.config.CubeContainers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CubeConfigurationTest {

    private static final String CONTENT = 
            "tomcat:\n" +
            "  image: tutum/tomcat:7.0\n" +
            "  exposedPorts: [8089/tcp]\n" +
            "  await:\n" +
            "    strategy: static\n" +
            "    ip: localhost\n" +
            "    ports: [8080, 8089]";

    private static final String CONTENT2 = 
            "tomcat2:\n" +
            "  image: tutum/tomcat:7.0\n" +
            "  exposedPorts: [8089/tcp]\n" +
            "  await:\n" +
            "    strategy: static\n" +
            "    ip: localhost\n" +
            "    ports: [8080, 8089]";

    private static final String DOCKER_COMPOSE_CONTENT = 
            "web:\n" +
            "  build: .\n" +
            "  ports:\n" +
            "   - \"5000:5000\"\n" +
            "  volumes:\n" +
            "   - .:/code\n" +
            "  links:\n" +
            "   - redis\n" +
            "redis:\n" +
            "  image: redis";

    private static final String OVERRIDE_CUSTOM =
            "tomcat:\n" +
            "  image: tutum/tomcat:8.0\n" +
            "  await:\n" +
            "    strategy: polling\n" +
            "  beforeStop: \n"+
            "    - copy:\n"+
            "        from: /test\n"+
            "        to: /tmp";

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Test
    public void should_override_custom_cube_properties() throws IOException {
        File newFile = testFolder.newFile("config.yaml");
        Files.write(Paths.get(newFile.toURI()), CONTENT.getBytes());

        Map<String, String> parameters = new HashMap<String, String>();

        parameters.put("serverVersion", "1.13");
        parameters.put("serverUri", "http://localhost:25123");
        parameters.put("dockerContainersFiles", newFile.toURI().toString());
        parameters.put("definitionFormat", DefinitionFormat.CUBE.name());
        parameters.put("cubeSpecificProperties", OVERRIDE_CUSTOM);

        CubeDockerConfiguration cubeConfiguration = CubeDockerConfiguration.fromMap(parameters);
        final CubeContainers dockerContainersContent = cubeConfiguration.getDockerContainersContent();

        final CubeContainer tomcat = dockerContainersContent.get("tomcat");
        assertThat(tomcat, is(notNullValue()));
        assertThat(tomcat.getImage().getTag(), is("7.0"));
        assertThat(tomcat.getAwait().getStrategy(), is("polling"));
        assertThat(tomcat.getBeforeStop().size(), is(1));

    }

    @Test
    public void should_merge_more_than_one_file_into_one() throws IOException {

        File newFile = testFolder.newFile("config.yaml");
        Files.write(Paths.get(newFile.toURI()), CONTENT.getBytes());

        File newFile2 = testFolder.newFile("config2.yaml");
        Files.write(Paths.get(newFile2.toURI()), CONTENT2.getBytes());

        Map<String, String> parameters = new HashMap<String, String>();

        parameters.put("serverVersion", "1.13");
        parameters.put("serverUri", "http://localhost:25123");
        parameters.put("dockerContainersFiles", newFile.toURI().toString() + ", " + newFile2.toURI().toString());
        parameters.put("definitionFormat", DefinitionFormat.COMPOSE.name());

        CubeDockerConfiguration cubeConfiguration = CubeDockerConfiguration.fromMap(parameters);
        final CubeContainers dockerContainersContent = cubeConfiguration.getDockerContainersContent();

        assertThat(dockerContainersContent.get("tomcat"), is(notNullValue()));
        assertThat(dockerContainersContent.get("tomcat2"), is(notNullValue()));
    }

    @Test
    public void should_load_docker_compose_format() {
        Map<String, String> parameters = new HashMap<String, String>();

        parameters.put("serverVersion", "1.13");
        parameters.put("serverUri", "http://localhost:25123");
        parameters.put("dockerContainers", DOCKER_COMPOSE_CONTENT);
        parameters.put("definitionFormat", DefinitionFormat.COMPOSE.name());

        CubeDockerConfiguration cubeConfiguration = CubeDockerConfiguration.fromMap(parameters);
        final CubeContainers dockerContainersContent = cubeConfiguration.getDockerContainersContent();

        CubeContainer actualWeb = dockerContainersContent.get("web");
        assertThat(actualWeb.getBuildImage(), is(notNullValue()));
        assertThat(actualWeb.getPortBindings(), is(notNullValue()));
        assertThat(actualWeb.getVolumes(), is(notNullValue()));
        assertThat(actualWeb.getLinks(), is(notNullValue()));

        CubeContainer actualRedis = dockerContainersContent.get("redis");
        assertThat(actualRedis.getImage(), is(notNullValue()));
    }

    @Test
    public void should_load_cube_configuration_from_cube_file_if_no_file_is_provided() {
        Map<String, String> parameters = new HashMap<String, String>();

        parameters.put("serverVersion", "1.13");
        parameters.put("serverUri", "http://localhost:25123");

        CubeDockerConfiguration cubeConfiguration = CubeDockerConfiguration.fromMap(parameters);

        CubeContainers dockerContainersContent = cubeConfiguration.getDockerContainersContent();
        CubeContainer actualTomcat = dockerContainersContent.get("tomcat");
        assertThat(actualTomcat, is(notNullValue()));

        String image = (String) actualTomcat.getImage().toImageRef();
        assertThat(image, is("tomcat:7.0"));
    }

    @Test
    public void should_parse_and_load_configuration_file() {

        Map<String, String> parameters = new HashMap<String, String>();

        parameters.put("serverVersion", "1.13");
        parameters.put("serverUri", "http://localhost:25123");
        parameters.put("dockerContainers", CONTENT);

        CubeDockerConfiguration cubeConfiguration = CubeDockerConfiguration.fromMap(parameters);
        assertThat(cubeConfiguration.getDockerServerUri(), is("http://localhost:25123"));
        assertThat(cubeConfiguration.getDockerServerVersion(), is("1.13"));

        CubeContainers dockerContainersContent = cubeConfiguration.getDockerContainersContent();
        CubeContainer actualTomcat = dockerContainersContent.get("tomcat");
        assertThat(actualTomcat, is(notNullValue()));

        String image = actualTomcat.getImage().toImageRef();
        assertThat(image, is("tutum/tomcat:7.0"));
    }

    @Test
    public void should_parse_and_load_configuration_file_from_container_configuration_file_and_property_set_file() throws IOException {

        File newFile = testFolder.newFile("config.yml");
        Files.write(Paths.get(newFile.toURI()), CONTENT.getBytes());

        File newFile2 = testFolder.newFile("config.demo.yml");
        Files.write(Paths.get(newFile2.toURI()), CONTENT2.getBytes());
        System.setProperty("cube.environment", "demo");
        Map<String, String> parameters = new HashMap<String, String>();

        parameters.put("serverVersion", "1.13");
        parameters.put("serverUri", "http://localhost:25123");
        parameters.put("dockerContainersFile", newFile.toURI().toString());

        CubeDockerConfiguration cubeConfiguration = CubeDockerConfiguration.fromMap(parameters);
        assertThat(cubeConfiguration.getDockerServerUri(), is("http://localhost:25123"));
        assertThat(cubeConfiguration.getDockerServerVersion(), is("1.13"));

        CubeContainers dockerContainersContent = cubeConfiguration.getDockerContainersContent();
        CubeContainer actualTomcat = dockerContainersContent.get("tomcat");
        assertThat(actualTomcat, is(notNullValue()));

        String image = (String) actualTomcat.getImage().toImageRef();
        assertThat(image, is("tutum/tomcat:7.0"));
        assertThat(dockerContainersContent.get("tomcat2"), is(notNullValue()));
    }

    @Test
    public void should_parse_and_load_configuration_file_from_container_configuration_file() throws IOException {

        File newFile = testFolder.newFile("config.yaml");
        Files.write(Paths.get(newFile.toURI()), CONTENT.getBytes());

        Map<String, String> parameters = new HashMap<String, String>();

        parameters.put("serverVersion", "1.13");
        parameters.put("serverUri", "http://localhost:25123");
        parameters.put("dockerContainersFile", newFile.toURI().toString());

        CubeDockerConfiguration cubeConfiguration = CubeDockerConfiguration.fromMap(parameters);
        assertThat(cubeConfiguration.getDockerServerUri(), is("http://localhost:25123"));
        assertThat(cubeConfiguration.getDockerServerVersion(), is("1.13"));

        CubeContainers dockerContainersContent = cubeConfiguration.getDockerContainersContent();
        CubeContainer actualTomcat = dockerContainersContent.get("tomcat");
        assertThat(actualTomcat, is(notNullValue()));

        String image = actualTomcat.getImage().toImageRef();
        assertThat(image, is("tutum/tomcat:7.0"));
    }

    @Test
    public void should_be_able_to_extend_and_override_toplevel() throws Exception {
        String config =
                "tomcat6:\n" +
                "  image: tutum/tomcat:6.0\n" +
                "  exposedPorts: [8089/tcp]\n" +
                "  await:\n" +
                "    strategy: static\n" +
                "    ip: localhost\n" +
                "    ports: [8080, 8089]\n" +
                "tomcat7:\n" +
                "  extends: tomcat6\n" +
                "  image: tutum/tomcat:7.0\n";

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("dockerContainers", config);
        CubeDockerConfiguration cubeConfiguration = CubeDockerConfiguration.fromMap(parameters);

        CubeContainer tomcat7 = cubeConfiguration.getDockerContainersContent().get("tomcat7");
        Assert.assertEquals("tutum/tomcat:7.0", tomcat7.getImage().toImageRef());
        Assert.assertTrue(tomcat7.getAwait() != null);
        Assert.assertEquals("8089/tcp", tomcat7.getExposedPorts().iterator().next().toString());
    }
}
