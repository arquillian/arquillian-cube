package org.arquillian.cube.docker.impl.client.containerobject.dsl;

import org.arquillian.cube.CubeController;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.docker.impl.model.LocalDockerNetworkRegistry;
import org.arquillian.cube.docker.impl.model.NetworkRegistry;
import org.arquillian.cube.docker.impl.requirement.RequiresDocker;
import org.arquillian.cube.docker.impl.requirement.RequiresDockerMachine;
import org.arquillian.cube.impl.model.LocalCubeRegistry;
import org.arquillian.cube.spi.CubeRegistry;
import org.jboss.arquillian.core.api.Injector;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@Category({RequiresDocker.class, RequiresDockerMachine.class})
public class ContainerNetworkObjectDslTestEnricherTest {

    private CubeRegistry cubeRegistry;
    private NetworkRegistry networkRegistry;
    private CubeController cubeController;
    private DockerClientExecutor dockerClientExecutor;

    @Before
    public void init() {
        cubeRegistry = new LocalCubeRegistry();
        networkRegistry = new LocalDockerNetworkRegistry();
        cubeController = mock(CubeController.class);
        dockerClientExecutor = mock(DockerClientExecutor.class);
    }

    @Test
    public void should_start_a_network() {

        ContainerNetworkObjectDslTestEnricher containerNetworkObjectDslTestEnricher = new ContainerNetworkObjectDslTestEnricher();
        Injector injector = mock(Injector.class);
        containerNetworkObjectDslTestEnricher.injectorInstance = () -> injector;
        containerNetworkObjectDslTestEnricher.networkRegistryInstance = () -> networkRegistry;
        containerNetworkObjectDslTestEnricher.dockerClientExecutorInstance = () -> dockerClientExecutor;

        when(injector.inject(any(Network.class))).then(invocation -> invocation.getArgumentAt(0, Network.class));
        when(dockerClientExecutor.createNetwork(eq("default"), any(org.arquillian.cube.docker.impl.client.config.Network.class))).thenReturn("default");

        containerNetworkObjectDslTestEnricher.enrich(new NetworkTest());

        verify(dockerClientExecutor).createNetwork(eq("default"), any(org.arquillian.cube.docker.impl.client.config.Network.class));
        assertThat(networkRegistry.getNetwork("default")).isNotNull();
    }

    @Test
    public void should_start_a_container() {

        Injector injector = new Injector() {
            @Override
            public <T> T inject(T target) {
                return target;
            }
        };

        ContainerNetworkObjectDslTestEnricher containerNetworkObjectDslTestEnricher = new ContainerNetworkObjectDslTestEnricher();
        containerNetworkObjectDslTestEnricher.injectorInstance = () -> injector;
        containerNetworkObjectDslTestEnricher.cubeRegistryInstance = () -> cubeRegistry;
        containerNetworkObjectDslTestEnricher.cubeControllerInstance = () -> cubeController;
        containerNetworkObjectDslTestEnricher.dockerClientExecutorInstance = () -> dockerClientExecutor;



        containerNetworkObjectDslTestEnricher.enrich(new SimpleContainerTest());

        verify(cubeController).create("mytomcat");
        verify(cubeController).start("mytomcat");

        assertThat(cubeRegistry.getCube("mytomcat")).isNotNull();

    }

    @Test
    public void should_start_multiple_containers_in_order() {

        Injector injector = new Injector() {
            @Override
            public <T> T inject(T target) {
                return target;
            }
        };

        ContainerNetworkObjectDslTestEnricher containerNetworkObjectDslTestEnricher = new ContainerNetworkObjectDslTestEnricher();
        containerNetworkObjectDslTestEnricher.injectorInstance = () -> injector;
        containerNetworkObjectDslTestEnricher.cubeRegistryInstance = () -> cubeRegistry;
        containerNetworkObjectDslTestEnricher.cubeControllerInstance = () -> cubeController;
        containerNetworkObjectDslTestEnricher.dockerClientExecutorInstance = () -> dockerClientExecutor;

        containerNetworkObjectDslTestEnricher.enrich(new MultipleContainerTest());

        InOrder inOrder = Mockito.inOrder(cubeController);
        inOrder.verify(cubeController).create("mytomcat3");
        inOrder.verify(cubeController).create("mytomcat1");
        inOrder.verify(cubeController).create("mytomcat2");

    }

    public static class MultipleContainerTest {
        @DockerContainer(order = 10)
        Container container1 = Container.withContainerName("mytomcat1")
                .fromImage("tomcat")
                .withPortBinding(8080)
                .build();

        @DockerContainer(order = 5)
        Container container2 = Container.withContainerName("mytomcat2")
                .fromImage("tomcat")
                .withPortBinding(8080)
                .build();

        @DockerContainer(order = 25)
        Container container3 = Container.withContainerName("mytomcat3")
                .fromImage("tomcat")
                .withPortBinding(8080)
                .build();
    }

    public static class SimpleContainerTest {

        @DockerContainer
        Container container = Container.withContainerName("mytomcat")
                                        .fromImage("tomcat")
                                        .withPortBinding(8080)
                                        .build();
    }

    public static class NetworkTest {

        @DockerNetwork
        Network network = Network.withDefaultDriver("default").build();

    }

}
