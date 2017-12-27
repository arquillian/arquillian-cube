package org.arquillian.cube.impl.client.container;

import java.util.List;
import org.arquillian.cube.docker.impl.requirement.RequiresDocker;
import org.arquillian.cube.docker.impl.requirement.RequiresDockerMachine;
import org.arquillian.cube.impl.util.TestPortBindings;
import org.arquillian.cube.spi.Binding;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.metadata.HasPortBindings;
import org.jboss.arquillian.config.descriptor.impl.ContainerDefImpl;
import org.jboss.arquillian.container.impl.ContainerImpl;
import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.deployment.Deployment;
import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.client.protocol.metadata.Servlet;
import org.jboss.arquillian.container.spi.context.ContainerContext;
import org.jboss.arquillian.container.spi.context.DeploymentContext;
import org.jboss.arquillian.container.spi.context.annotation.ContainerScoped;
import org.jboss.arquillian.container.spi.context.annotation.DeploymentScoped;
import org.jboss.arquillian.container.test.AbstractContainerTestBase;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.spi.Manager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@Category({RequiresDocker.class, RequiresDockerMachine.class})
public class ProtocolMetaDataUpdaterTestCase extends AbstractContainerTestBase {

    private static final String CUBE_CONTAINER_NAME = "test";
    private static final String LOCALHOST = "localhost";
    private static final String GATEWAY_IP = "10.0.0.1";
    private static final Integer EXPOSED_PORT = 80;
    private static final Integer BOUND_PORT = 90;

    @Mock
    private Deployment deployment;

    @Mock
    private DeployableContainer<?> deployableContainer;

    @Mock
    private CubeRegistry registry;

    @Mock
    private Cube cube;

    @Override
    protected void addExtensions(List<Class<?>> extensions) {
        super.addExtensions(extensions);
        extensions.add(ProtocolMetadataUpdater.class);
    }

    @Override
    protected void startContexts(Manager manager) {
        super.startContexts(manager);
        manager.getContext(ContainerContext.class).activate(CUBE_CONTAINER_NAME);
        manager.getContext(DeploymentContext.class).activate(deployment);
    }

    @Before
    public void setup() {
        Mockito.when(registry.getCube(CUBE_CONTAINER_NAME)).thenReturn(cube);
        bind(ApplicationScoped.class, CubeRegistry.class, registry);
    }

    @Test // equal ip, different ports
    public void shouldUpdateWithPortFromDocker() throws Exception {

        Binding binding = new Binding(LOCALHOST);
        binding.addPortBinding(EXPOSED_PORT, BOUND_PORT);
        Mockito.when(cube.getMetadata(HasPortBindings.class)).thenReturn(new TestPortBindings(binding));

        bind(ContainerScoped.class,
            Container.class,
            new ContainerImpl(CUBE_CONTAINER_NAME, deployableContainer, new ContainerDefImpl("arquillian.xml")));

        ProtocolMetaData metadata = new ProtocolMetaData();
        metadata.addContext(new HTTPContext(LOCALHOST, EXPOSED_PORT).add(new Servlet("A", "B")));

        bind(DeploymentScoped.class, ProtocolMetaData.class, metadata);
        fire(metadata);

        ProtocolMetaData updated = getManager().getContext(DeploymentContext.class)
            .getObjectStore().get(ProtocolMetaData.class);

        Assert.assertEquals(BOUND_PORT.intValue(), updated.getContexts(HTTPContext.class).iterator().next().getPort());
        Assert.assertEquals(LOCALHOST, updated.getContexts(HTTPContext.class).iterator().next().getHost());
        assertEventFired(ProtocolMetaData.class, 1); // twice, but original fire is intercepted and never hit the Counter
    }

    @Test // equal ports, different ip
    public void shouldUpdateWithIPFromDocker() throws Exception {

        Binding binding = new Binding(GATEWAY_IP);
        binding.addPortBinding(EXPOSED_PORT, EXPOSED_PORT);
        Mockito.when(cube.getMetadata(HasPortBindings.class)).thenReturn(new TestPortBindings(binding));

        bind(ContainerScoped.class,
            Container.class,
            new ContainerImpl(CUBE_CONTAINER_NAME, deployableContainer, new ContainerDefImpl("arquillian.xml")));

        ProtocolMetaData metadata = new ProtocolMetaData();
        metadata.addContext(new HTTPContext(LOCALHOST, EXPOSED_PORT).add(new Servlet("A", "B")));

        bind(DeploymentScoped.class, ProtocolMetaData.class, metadata);
        fire(metadata);

        ProtocolMetaData updated = getManager().getContext(DeploymentContext.class)
            .getObjectStore().get(ProtocolMetaData.class);

        Assert.assertEquals(EXPOSED_PORT.intValue(), updated.getContexts(HTTPContext.class).iterator().next().getPort());
        Assert.assertEquals(GATEWAY_IP, updated.getContexts(HTTPContext.class).iterator().next().getHost());
        assertEventFired(ProtocolMetaData.class, 1); // twice, but original fire is intercepted and never hit the Counter
    }

    @Test
    public void shouldNotUpdateIfContainerNotMapped() throws Exception {
        Binding binding = new Binding(GATEWAY_IP);
        binding.addPortBinding(EXPOSED_PORT, EXPOSED_PORT);
        Mockito.when(cube.getMetadata(HasPortBindings.class)).thenReturn(new TestPortBindings(binding));

        bind(ContainerScoped.class,
            Container.class,
            new ContainerImpl("_UNMAPPED_", deployableContainer, new ContainerDefImpl("arquillian.xml")));

        ProtocolMetaData metadata = new ProtocolMetaData();
        metadata.addContext(new HTTPContext(LOCALHOST, EXPOSED_PORT).add(new Servlet("A", "B")));

        bind(DeploymentScoped.class, ProtocolMetaData.class, metadata);
        fire(metadata);

        ProtocolMetaData updated = getManager().getContext(DeploymentContext.class)
            .getObjectStore().get(ProtocolMetaData.class);

        Assert.assertEquals(metadata.getContexts(HTTPContext.class).iterator().next(),
            updated.getContexts(HTTPContext.class).iterator().next());
        assertEventFired(ProtocolMetaData.class, 1);
    }
}
