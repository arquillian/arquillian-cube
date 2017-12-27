package org.arquillian.cube.docker.impl.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.arquillian.cube.docker.impl.client.config.Network;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.docker.impl.model.LocalDockerNetworkRegistry;
import org.arquillian.cube.docker.impl.model.NetworkRegistry;
import org.arquillian.cube.docker.impl.requirement.RequiresDocker;
import org.arquillian.cube.docker.impl.requirement.RequiresDockerMachine;
import org.arquillian.cube.spi.CubeConfiguration;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.test.AbstractManagerTestBase;
import org.jboss.arquillian.test.spi.event.suite.AfterSuite;
import org.jboss.arquillian.test.spi.event.suite.BeforeSuite;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@Category({RequiresDocker.class, RequiresDockerMachine.class})
public class NetworkLifecycleControllerTest extends AbstractManagerTestBase {

    @Override
    protected void addExtensions(List<Class<?>> extensions) {
        extensions.add(NetworkLifecycleController.class);
        super.addExtensions(extensions);
    }

    @Test
    public void shouldStartNetworks() {

        DockerClientExecutor executor = Mockito.mock(DockerClientExecutor.class);

        String config =
            "networks:\n" +
                "  mynetwork:\n " +
                "    driver: bridge\n" +
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
        parameters.put("definitionFormat", DefinitionFormat.CUBE.name());

        CubeConfiguration cubeConfiguration = CubeConfiguration.fromMap(new HashMap<String, String>());
        bind(ApplicationScoped.class, CubeConfiguration.class, cubeConfiguration);

        CubeDockerConfiguration dockerConfiguration = CubeDockerConfiguration.fromMap(parameters, null);
        bind(ApplicationScoped.class, CubeDockerConfiguration.class, dockerConfiguration);

        final LocalDockerNetworkRegistry localDockerNetworkRegistry = new LocalDockerNetworkRegistry();
        bind(ApplicationScoped.class, NetworkRegistry.class, localDockerNetworkRegistry);
        bind(ApplicationScoped.class, DockerClientExecutor.class, executor);

        fire(new BeforeSuite());
        Mockito.verify(executor).createNetwork(Matchers.eq("mynetwork"), Matchers.any(Network.class));
    }

    @Test
    public void shouldStopNetworks() {

        DockerClientExecutor executor = Mockito.mock(DockerClientExecutor.class);

        String config =
            "networks:\n" +
                "  mynetwork:\n " +
                "    driver: bridge\n" +
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
        parameters.put("definitionFormat", DefinitionFormat.CUBE.name());

        CubeConfiguration cubeConfiguration = CubeConfiguration.fromMap(new HashMap<String, String>());
        bind(ApplicationScoped.class, CubeConfiguration.class, cubeConfiguration);

        CubeDockerConfiguration dockerConfiguration = CubeDockerConfiguration.fromMap(parameters, null);
        bind(ApplicationScoped.class, CubeDockerConfiguration.class, dockerConfiguration);

        final LocalDockerNetworkRegistry localDockerNetworkRegistry = new LocalDockerNetworkRegistry();
        localDockerNetworkRegistry.addNetwork("mynetwork", new Network());
        bind(ApplicationScoped.class, NetworkRegistry.class, localDockerNetworkRegistry);
        bind(ApplicationScoped.class, DockerClientExecutor.class, executor);

        fire(new AfterSuite());
        Mockito.verify(executor).removeNetwork(Matchers.eq("mynetwork"));
    }
}
