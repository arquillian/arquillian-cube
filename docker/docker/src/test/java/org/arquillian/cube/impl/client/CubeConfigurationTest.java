package org.arquillian.cube.impl.client;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CubeConfigurationTest {

    private static final String CONTENT = "tomcat:\n" +
            "  image: tutum/tomcat:7.0\n" +
            "  exposedPorts: [8089/tcp]\n" +
            "  await:\n" +
            "    strategy: static\n" +
            "    ip: localhost\n" +
            "    ports: [8080, 8089]";

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Test
    public void should_load_cube_configuration_from_cube_file_if_no_file_is_provided() {
        Map<String, String> parameters = new HashMap<String, String>();

        parameters.put("serverVersion", "1.13");
        parameters.put("serverUri", "http://localhost:25123");

        CubeConfiguration cubeConfiguration = CubeConfiguration.fromMap(parameters);

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

        CubeConfiguration cubeConfiguration = CubeConfiguration.fromMap(parameters);
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
    public void should_parse_and_load_configuration_file_from_container_configuration_file() throws IOException {

        File newFile = testFolder.newFile("config.yaml");
        Files.write(Paths.get(newFile.toURI()), CONTENT.getBytes());

        Map<String, String> parameters = new HashMap<String, String>();

        parameters.put("serverVersion", "1.13");
        parameters.put("serverUri", "http://localhost:25123");
        parameters.put("dockerContainersFile", newFile.getAbsolutePath());

        CubeConfiguration cubeConfiguration = CubeConfiguration.fromMap(parameters);
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
    public void should_parse_empty_autostart() throws Exception {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("autoStartContainers", "");

        CubeConfiguration cubeConfiguration = CubeConfiguration.fromMap(parameters);
        Assert.assertNotNull(cubeConfiguration.getAutoStartContainers());
        Assert.assertEquals(0, cubeConfiguration.getAutoStartContainers().length);
    }

    @Test
    public void should_parse_empty_values_autostart() throws Exception {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("autoStartContainers", "  ,   ");

        CubeConfiguration cubeConfiguration = CubeConfiguration.fromMap(parameters);
        Assert.assertNotNull(cubeConfiguration.getAutoStartContainers());
        Assert.assertEquals(0, cubeConfiguration.getAutoStartContainers().length);
    }

    @Test
    public void should_parse_trim_autostart() throws Exception {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("autoStartContainers", "a , b ");

        CubeConfiguration cubeConfiguration = CubeConfiguration.fromMap(parameters);
        Assert.assertNotNull(cubeConfiguration.getAutoStartContainers());
        Assert.assertEquals(2, cubeConfiguration.getAutoStartContainers().length);
        Assert.assertEquals("a", cubeConfiguration.getAutoStartContainers()[0]);
        Assert.assertEquals("b", cubeConfiguration.getAutoStartContainers()[1]);
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
        CubeConfiguration cubeConfiguration = CubeConfiguration.fromMap(parameters);

        Map<String, Object> tomcat7 = (Map<String, Object>)cubeConfiguration.getDockerContainersContent().get("tomcat7");
        Assert.assertEquals("tutum/tomcat:7.0", tomcat7.get("image").toString());
        Assert.assertTrue(tomcat7.containsKey("await"));
        Assert.assertEquals("8089/tcp", ((List<Object>)tomcat7.get("exposedPorts")).get(0).toString());
    }
}
