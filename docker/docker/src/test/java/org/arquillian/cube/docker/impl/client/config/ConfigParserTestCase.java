package org.arquillian.cube.docker.impl.client.config;

import java.util.Iterator;
import org.arquillian.cube.docker.impl.util.ConfigUtil;
import org.junit.Assert;
import org.junit.Test;

public class ConfigParserTestCase {

    private static final String CONTENT =
        "tomcat:\n" +
            "  image: tutum/tomcat:7.0\n" +
            "  buildImage:\n" +
            "      dockerfileLocation: src/test/resources/undertow\n" +
            "  exposedPorts: [8089/tcp]\n" +
            "  portBindings: [1521/tcp, 'localhost:8181->81/tcp']\n" +
            "  await:\n" +
            "    strategy: static\n" +
            "    ip: localhost\n" +
            "    ports: [8080, 8089]";

    @Test
    public void shouldBeAbleToLoadStrategy() throws Exception {

        DockerCompositions containers = ConfigUtil.load(CONTENT);
        CubeContainer container = containers.get("tomcat");

        Assert.assertEquals("tutum/tomcat", container.getImage().getName());
        Assert.assertEquals("7.0", container.getImage().getTag());

        Assert.assertEquals(1, container.getExposedPorts().size());
        ExposedPort exposedPort = container.getExposedPorts().iterator().next();
        Assert.assertEquals(8089, exposedPort.getExposed());
        Assert.assertEquals("tcp", exposedPort.getType());

        Assert.assertEquals(2, container.getPortBindings().size());
        Iterator<PortBinding> portBindingIterator = container.getPortBindings().iterator();
        PortBinding portBinding1 = portBindingIterator.next();
        PortBinding portBinding2 = portBindingIterator.next();
        Assert.assertEquals(1521, portBinding1.getExposedPort().getExposed());
        Assert.assertEquals("tcp", portBinding1.getExposedPort().getType());

        Assert.assertEquals(8181, portBinding2.getBound());
        Assert.assertEquals("localhost", portBinding2.getHost());
        Assert.assertEquals(81, portBinding2.getExposedPort().getExposed());
        Assert.assertEquals("tcp", portBinding2.getExposedPort().getType());

        Assert.assertEquals("static", container.getAwait().getStrategy());
        Assert.assertEquals("localhost", container.getAwait().getIp());
        Assert.assertEquals(2, container.getAwait().getPorts().size());

        System.out.println(ConfigUtil.dump(containers));
    }
}
