package org.arquillian.cube.docker.impl.util;

import java.util.HashMap;
import java.util.Map;
import org.arquillian.cube.docker.impl.client.CubeDockerConfiguration;
import org.arquillian.cube.docker.impl.client.DefinitionFormat;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SinglePortBindResolverTest {

    @Test
    public void should_resolve_single_bind_port() {
        String content =
            "tomcat:\n" +
                "  image: tutum/tomcat:8.0\n" +
                "  portBindings: [8080/tcp]\n" +
                "  links:\n" +
                "    - ping\n" +
                "ping:\n" +
                "  image: hashicorp/http-echo\n" +
                "  exposedPorts: [8089/tcp]\n" +
                "storage:\n" +
                "  image: tutum/mongodb";
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("dockerContainers", content);
        parameters.put("definitionFormat", DefinitionFormat.CUBE.name());
        CubeDockerConfiguration cubeConfiguration = CubeDockerConfiguration.fromMap(parameters, null);

        final int bindPort = SinglePortBindResolver.resolveBindPort(cubeConfiguration);
        assertThat(bindPort, is(8080));
    }

    @Test(expected = IllegalArgumentException.class)
    public void should_throw_exception_with_resolve_two_bind_port() {
        String content =
            "tomcat:\n" +
                "  image: tutum/tomcat:8.0\n" +
                "  portBindings: [8080/tcp, 8081/tcp]\n" +
                "  links:\n" +
                "    - ping\n" +
                "ping:\n" +
                "  image: hashicorp/http-echo\n" +
                "  exposedPorts: [8089/tcp]\n" +
                "storage:\n" +
                "  image: tutum/mongodb";
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("dockerContainers", content);
        parameters.put("definitionFormat", DefinitionFormat.CUBE.name());
        CubeDockerConfiguration cubeConfiguration = CubeDockerConfiguration.fromMap(parameters, null);

        final int bindPort = SinglePortBindResolver.resolveBindPort(cubeConfiguration);
    }

    @Test(expected = IllegalArgumentException.class)
    public void should_throw_exception_with_resolve_two_bind_port_from_different_containers() {
        String content =
            "tomcat:\n" +
                "  image: tutum/tomcat:8.0\n" +
                "  portBindings: [8080/tcp]\n" +
                "  links:\n" +
                "    - ping\n" +
                "ping:\n" +
                "  image: hashicorp/http-echo\n" +
                "  portBindings: [8081/tcp]\n" +
                "storage:\n" +
                "  image: tutum/mongodb";
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("dockerContainers", content);
        parameters.put("definitionFormat", DefinitionFormat.CUBE.name());
        CubeDockerConfiguration cubeConfiguration = CubeDockerConfiguration.fromMap(parameters, null);

        final int bindPort = SinglePortBindResolver.resolveBindPort(cubeConfiguration);
    }

    @Test
    public void should_resolve_two_bind_port_from_different_containers_with_exclusions() {
        String content =
            "tomcat:\n" +
                "  image: tutum/tomcat:8.0\n" +
                "  portBindings: [8080/tcp]\n" +
                "  links:\n" +
                "    - ping\n" +
                "ping:\n" +
                "  image: hashicorp/http-echo\n" +
                "  portBindings: [8081/tcp]\n" +
                "storage:\n" +
                "  image: tutum/mongodb";
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("dockerContainers", content);
        parameters.put("definitionFormat", DefinitionFormat.CUBE.name());
        CubeDockerConfiguration cubeConfiguration = CubeDockerConfiguration.fromMap(parameters, null);

        final int bindPort = SinglePortBindResolver.resolveBindPort(cubeConfiguration, "ping");
        assertThat(bindPort, is(8080));
    }

    @Test
    public void should_resolve_two_bind_port_from_different_containers_with_exposed_port() {
        String content =
            "tomcat:\n" +
                "  image: tutum/tomcat:8.0\n" +
                "  portBindings: [8080/tcp]\n" +
                "  links:\n" +
                "    - ping\n" +
                "ping:\n" +
                "  image: hashicorp/http-echo\n" +
                "  portBindings: [8081/tcp]\n" +
                "storage:\n" +
                "  image: tutum/mongodb";
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("dockerContainers", content);
        parameters.put("definitionFormat", DefinitionFormat.CUBE.name());
        CubeDockerConfiguration cubeConfiguration = CubeDockerConfiguration.fromMap(parameters, null);

        final int bindPort = SinglePortBindResolver.resolveBindPort(cubeConfiguration, 8080);
        assertThat(bindPort, is(8080));
    }

    @Test(expected = IllegalArgumentException.class)
    public void should_throw_exception_when_resolve_two_bind_port_from_different_containers_with_same_exposed_port() {
        String content =
            "tomcat:\n" +
                "  image: tutum/tomcat:8.0\n" +
                "  portBindings: [8080/tcp]\n" +
                "  links:\n" +
                "    - ping\n" +
                "ping:\n" +
                "  image: hashicorp/http-echo\n" +
                "  portBindings: [8080/tcp]\n" +
                "storage:\n" +
                "  image: tutum/mongodb";
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("dockerContainers", content);
        parameters.put("definitionFormat", DefinitionFormat.CUBE.name());
        CubeDockerConfiguration cubeConfiguration = CubeDockerConfiguration.fromMap(parameters, null);

        final int bindPort = SinglePortBindResolver.resolveBindPort(cubeConfiguration, 8080);
    }

    @Test
    public void should_resolve_two_bind_port_from_different_containers_with_exposed_port_value_if_no_matches() {
        String content =
            "tomcat:\n" +
                "  image: tutum/tomcat:8.0\n" +
                "  portBindings: [8080/tcp]\n" +
                "  links:\n" +
                "    - ping\n" +
                "ping:\n" +
                "  image: hashicorp/http-echo\n" +
                "  portBindings: [8081/tcp]\n" +
                "storage:\n" +
                "  image: tutum/mongodb";
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("dockerContainers", content);
        parameters.put("definitionFormat", DefinitionFormat.CUBE.name());
        CubeDockerConfiguration cubeConfiguration = CubeDockerConfiguration.fromMap(parameters, null);

        final int bindPort = SinglePortBindResolver.resolveBindPort(cubeConfiguration, 8082);
        assertThat(bindPort, is(8082));
    }
}
