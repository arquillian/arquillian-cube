package org.arquillian.cube.docker.impl.client;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.junit.Assert.assertThat;

public class CubeConfigurationTest {

    private static final String CONTENT = "tomcat:\n" +
            "  image: tutum/tomcat:7.0\n" +
            "  exposedPorts: [8089/tcp]\n" +
            "  await:\n" +
            "    strategy: static\n" +
            "    ip: localhost\n" +
            "    ports: [8080, 8089]";

    private static final String CONTENT2 = "tomcat2:\n" +
            "  image: tutum/tomcat:7.0\n" +
            "  exposedPorts: [8089/tcp]\n" +
            "  await:\n" +
            "    strategy: static\n" +
            "    ip: localhost\n" +
            "    ports: [8080, 8089]";

    private static final String DOCKER_COMPOSE_CONTENT = "web:\n" +
            "  build: .\n" +
            "  ports:\n" +
            "   - \"5000:5000\"\n" +
            "  volumes:\n" +
            "   - .:/code\n" +
            "  links:\n" +
            "   - redis\n" +
            "redis:\n" +
            "  image: redis";

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Test
    public void should_merge_more_than_one_file_into_one() throws IOException {

        File newFile = testFolder.newFile("config.yaml");
        Files.write(Paths.get(newFile.toURI()), CONTENT.getBytes());

        File newFile2 = testFolder.newFile("config2.yaml");
        Files.write(Paths.get(newFile2.toURI()), CONTENT2.getBytes());

        Map<String, String> parameters = new HashMap<String, String>();

        parameters.put("serverVersion", "1.13");
        parameters.put("serverUri", "http://localhost:25123");
        parameters.put("dockerContainersFiles", newFile.getAbsolutePath() + ", " + newFile2.getAbsolutePath());
        parameters.put("definitionFormat", DefinitionFormat.COMPOSE.name());

        CubeDockerConfiguration cubeConfiguration = CubeDockerConfiguration.fromMap(parameters);
        final Map<String, Object> dockerContainersContent = cubeConfiguration.getDockerContainersContent();

        assertThat(dockerContainersContent.containsKey("tomcat"), is(true));
        assertThat(dockerContainersContent.containsKey("tomcat2"), is(true));
    }

    @Test
    public void should_load_docker_compose_format() {
        Map<String, String> parameters = new HashMap<String, String>();

        parameters.put("serverVersion", "1.13");
        parameters.put("serverUri", "http://localhost:25123");
        parameters.put("dockerContainers", DOCKER_COMPOSE_CONTENT);
        parameters.put("definitionFormat", DefinitionFormat.COMPOSE.name());

        CubeDockerConfiguration cubeConfiguration = CubeDockerConfiguration.fromMap(parameters);
        Map<String, Object> dockerContainersContent = cubeConfiguration.getDockerContainersContent();
        @SuppressWarnings("unchecked")
        Map<String, Object> actualWeb = (Map<String, Object>) dockerContainersContent.get("web");
        assertThat(actualWeb, hasKey("buildImage"));
        assertThat(actualWeb, hasKey("portBindings"));
        assertThat(actualWeb, hasKey("volumes"));
        assertThat(actualWeb, hasKey("links"));

        @SuppressWarnings("unchecked")
        Map<String, Object> actualRedis = (Map<String, Object>) dockerContainersContent.get("redis");
        assertThat(actualRedis, hasKey("image"));
    }

    @Test
    public void should_load_cube_configuration_from_cube_file_if_no_file_is_provided() {
        Map<String, String> parameters = new HashMap<String, String>();

        parameters.put("serverVersion", "1.13");
        parameters.put("serverUri", "http://localhost:25123");

        CubeDockerConfiguration cubeConfiguration = CubeDockerConfiguration.fromMap(parameters);

        Map<String, Object> dockerContainersContent = cubeConfiguration.getDockerContainersContent();
        @SuppressWarnings("unchecked")
        Map<String, Object> actualTomcat = (Map<String, Object>) dockerContainersContent.get("tomcat");
        assertThat(actualTomcat, is(notNullValue()));

        String image = (String) actualTomcat.get("image");
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

        Map<String, Object> dockerContainersContent = cubeConfiguration.getDockerContainersContent();
        @SuppressWarnings("unchecked")
        Map<String, Object> actualTomcat = (Map<String, Object>) dockerContainersContent.get("tomcat");
        assertThat(actualTomcat, is(notNullValue()));

        String image = (String) actualTomcat.get("image");
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
        parameters.put("dockerContainersFile", newFile.getAbsolutePath());

        CubeDockerConfiguration cubeConfiguration = CubeDockerConfiguration.fromMap(parameters);
        assertThat(cubeConfiguration.getDockerServerUri(), is("http://localhost:25123"));
        assertThat(cubeConfiguration.getDockerServerVersion(), is("1.13"));

        Map<String, Object> dockerContainersContent = cubeConfiguration.getDockerContainersContent();
        @SuppressWarnings("unchecked")
        Map<String, Object> actualTomcat = (Map<String, Object>) dockerContainersContent.get("tomcat");
        assertThat(actualTomcat, is(notNullValue()));

        String image = (String) actualTomcat.get("image");
        assertThat(image, is("tutum/tomcat:7.0"));
        assertThat(dockerContainersContent.containsKey("tomcat2"), is(true));
    }

    @Test
    public void should_parse_and_load_configuration_file_from_container_configuration_file() throws IOException {

        File newFile = testFolder.newFile("config.yaml");
        Files.write(Paths.get(newFile.toURI()), CONTENT.getBytes());

        Map<String, String> parameters = new HashMap<String, String>();

        parameters.put("serverVersion", "1.13");
        parameters.put("serverUri", "http://localhost:25123");
        parameters.put("dockerContainersFile", newFile.getAbsolutePath());

        CubeDockerConfiguration cubeConfiguration = CubeDockerConfiguration.fromMap(parameters);
        assertThat(cubeConfiguration.getDockerServerUri(), is("http://localhost:25123"));
        assertThat(cubeConfiguration.getDockerServerVersion(), is("1.13"));

        Map<String, Object> dockerContainersContent = cubeConfiguration.getDockerContainersContent();
        @SuppressWarnings("unchecked")
        Map<String, Object> actualTomcat = (Map<String, Object>) dockerContainersContent.get("tomcat");
        assertThat(actualTomcat, is(notNullValue()));

        String image = (String) actualTomcat.get("image");
        assertThat(image, is("tutum/tomcat:7.0"));
    }

    @SuppressWarnings("unchecked")
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

        Map<String, Object> tomcat7 = (Map<String, Object>)cubeConfiguration.getDockerContainersContent().get("tomcat7");
        Assert.assertEquals("tutum/tomcat:7.0", tomcat7.get("image").toString());
        Assert.assertTrue(tomcat7.containsKey("await"));
        Assert.assertEquals("8089/tcp", ((List<Object>)tomcat7.get("exposedPorts")).get(0).toString());
    }
}
