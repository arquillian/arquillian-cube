package org.arquillian.cube.docker.impl.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.arquillian.cube.spi.Node;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class AutomaticResolutionAutostartParserTest {

    @Test
    public void shouldStartNoneDeployableContainersWithRegisteredNetwork() {

        List<String> deployableContainer = new ArrayList<>();
        deployableContainer.add("tomcat");

        String config = "networks:\n"
            +
            "  mynetwork:\n"
            +
            "    driver: bridge\n"
            +
            "tomcat:\n"
            +
            "  image: tutum/tomcat:7.0\n"
            +
            "  exposedPorts: [8089/tcp]\n"
            +
            "  env: [TOMCAT_PASS=mypass, \"CATALINA_OPTS=-Djava.security.egd=file:/dev/./urandom\", JAVA_OPTS=-Djava.rmi.server.hostname=dockerServerIp -Dcom.sun.management.jmxremote.rmi.port=8088 -Dcom.sun.management.jmxremote.port=8089 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false]\n"
            +
            "  portBindings: [8089/tcp, 8088/tcp, 8081->8080/tcp]\n"
            +
            "  networkMode: mynetwork\n"
            +
            "pingpong:\n"
            +
            "  image: hashicorp/http-echo\n"
            +
            "  exposedPorts: [8080/tcp]\n"
            +
            "  portBindings: [8080->8080/tcp]\n"
            +
            "  networkMode: mynetwork";

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("dockerContainers", config);
        parameters.put("definitionFormat", DefinitionFormat.CUBE.name());
        CubeDockerConfiguration cubeConfiguration = CubeDockerConfiguration.fromMap(parameters, null);

        AutomaticResolutionNetworkAutoStartParser automaticResolutionNetworkAutoStartParser =
            new AutomaticResolutionNetworkAutoStartParser(deployableContainer,
                cubeConfiguration.getDockerContainersContent());
        final Map<String, Node> parse = automaticResolutionNetworkAutoStartParser.parse();

        assertThat(parse.get("pingpong"), is(notNullValue()));
        assertThat(parse.get("tomcat"), is(nullValue()));
    }

    @Test
    public void shouldNotStartManualContainers() {

        String config =
            "tomcat:\n"
                +
                "  image: tutum/tomcat:7.0\n"
                +
                "  exposedPorts: [8089/tcp]\n"
                +
                "  env: [TOMCAT_PASS=mypass, \"CATALINA_OPTS=-Djava.security.egd=file:/dev/./urandom\", JAVA_OPTS=-Djava.rmi.server.hostname=dockerServerIp -Dcom.sun.management.jmxremote.rmi.port=8088 -Dcom.sun.management.jmxremote.port=8089 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false]\n"
                +
                "  portBindings: [8089/tcp, 8088/tcp, 8081->8080/tcp]\n"
                +
                "pingpong:\n"
                +
                "  image: hashicorp/http-echo\n"
                +
                "  exposedPorts: [8080/tcp]\n"
                +
                "  portBindings: [8080->8080/tcp]\n"
                +
                "  manual: true";

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("dockerContainers", config);
        parameters.put("definitionFormat", DefinitionFormat.CUBE.name());
        CubeDockerConfiguration cubeConfiguration = CubeDockerConfiguration.fromMap(parameters, null);

        RegularExpressionAutoStartParser regularExpressionAutoStartParser =
            new RegularExpressionAutoStartParser("regexp:.*", cubeConfiguration.getDockerContainersContent());
        final Map<String, Node> parse = regularExpressionAutoStartParser.parse();

        assertThat(parse.get("tomcat"), is(notNullValue()));
        assertThat(parse.get("pingping"), is(nullValue()));
    }
}
