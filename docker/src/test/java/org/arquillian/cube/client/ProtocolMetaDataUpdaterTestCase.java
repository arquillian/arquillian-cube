package org.arquillian.cube.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ProtocolMetaDataUpdaterTestCase extends AbstractContainerTestBase {

    @Mock
    private Deployment deployment;

    @Mock
    private DeployableContainer<?> deployableContainer;

    @Override
    protected void addExtensions(List<Class<?>> extensions) {
        super.addExtensions(extensions);
        extensions.add(ProtocolMetadataUpdater.class);
    }

    @Override
    protected void startContexts(Manager manager) {
        super.startContexts(manager);
        manager.getContext(ContainerContext.class).activate("test");
        manager.getContext(DeploymentContext.class).activate(deployment);
    }

    @Test
    public void shouldNotUpdateWhenPortsAreEqual() throws Exception {
        Map<String, String> configuration = new HashMap<String, String>();
        configuration.put("dockerContainers", "test:\n  portBindings:\n  - exposedPort: 80/tcp\n    ports: 80\n");

        bind(ContainerScoped.class,
             Container.class,
             new ContainerImpl("test", deployableContainer, new ContainerDefImpl("arquillian.xml")));

        getManager().bind(ApplicationScoped.class, CubeConfiguration.class, CubeConfiguration.fromMap(configuration));

        ProtocolMetaData metadata = new ProtocolMetaData();
        metadata.addContext(new HTTPContext("localhost", 80).add(new Servlet("A", "B")));

        bind(DeploymentScoped.class, ProtocolMetaData.class, metadata);
        fire(metadata);

        ProtocolMetaData updated = getManager().getContext(DeploymentContext.class)
                .getObjectStore().get(ProtocolMetaData.class);

        Assert.assertEquals(metadata.getContexts(HTTPContext.class).iterator().next(), updated.getContexts(HTTPContext.class).iterator().next());
        assertEventFired(ProtocolMetaData.class, 1);
    }

    @Test
    public void shouldUpdateWhenPortsAreNotEqual() throws Exception {
        Map<String, String> configuration = new HashMap<String, String>();
        configuration.put("dockerContainers", "test:\n  portBindings:\n  - exposedPort: 80/tcp\n    port: 90\n");

        bind(ContainerScoped.class,
             Container.class,
             new ContainerImpl("test", deployableContainer, new ContainerDefImpl("arquillian.xml")));

        getManager().bind(ApplicationScoped.class, CubeConfiguration.class, CubeConfiguration.fromMap(configuration));

        ProtocolMetaData metadata = new ProtocolMetaData();
        metadata.addContext(new HTTPContext("localhost", 80).add(new Servlet("A", "B")));

        bind(DeploymentScoped.class, ProtocolMetaData.class, metadata);
        fire(metadata);

        ProtocolMetaData updated = getManager().getContext(DeploymentContext.class)
                .getObjectStore().get(ProtocolMetaData.class);

        Assert.assertEquals(90, updated.getContexts(HTTPContext.class).iterator().next().getPort());
        assertEventFired(ProtocolMetaData.class, 1); // twice, but original fire is intercepted and never hit the Counter
    }

    @Test
    public void shouldNotFailOnMissingContainer() throws Exception {
        Map<String, String> configuration = new HashMap<String, String>();
        configuration.put("dockerContainers", "test:\n  portBindings:\n  - exposedPort: 80/tcp\n    port: 90\n");

        getManager().bind(ApplicationScoped.class, CubeConfiguration.class, CubeConfiguration.fromMap(configuration));

        ProtocolMetaData metadata = new ProtocolMetaData();
        metadata.addContext(new HTTPContext("localhost", 80).add(new Servlet("A", "B")));

        bind(DeploymentScoped.class, ProtocolMetaData.class, metadata);
        fire(metadata);

        ProtocolMetaData updated = getManager().getContext(DeploymentContext.class)
                .getObjectStore().get(ProtocolMetaData.class);

        Assert.assertEquals(80, updated.getContexts(HTTPContext.class).iterator().next().getPort());
        assertEventFired(ProtocolMetaData.class, 1);
    }

    @Test
    public void shouldNotFailOnMissingCubeConfiguration() throws Exception {
        bind(ContainerScoped.class,
                Container.class,
                new ContainerImpl("test", deployableContainer, new ContainerDefImpl("arquillian.xml")));

        ProtocolMetaData metadata = new ProtocolMetaData();
        metadata.addContext(new HTTPContext("localhost", 80).add(new Servlet("A", "B")));

        bind(DeploymentScoped.class, ProtocolMetaData.class, metadata);
        fire(metadata);

        ProtocolMetaData updated = getManager().getContext(DeploymentContext.class)
                .getObjectStore().get(ProtocolMetaData.class);

        Assert.assertEquals(80, updated.getContexts(HTTPContext.class).iterator().next().getPort());
        assertEventFired(ProtocolMetaData.class, 1);
    }
}
