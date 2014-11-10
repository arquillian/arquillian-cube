package org.arquillian.cube.client;

import java.util.List;

import org.arquillian.cube.docker.DockerClientExecutor;
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
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse.HostConfig;
import com.github.dockerjava.api.command.InspectContainerResponse.NetworkSettings;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Ports.Binding;

@RunWith(MockitoJUnitRunner.class)
public class ProtocolMetaDataUpdaterTestCase extends AbstractContainerTestBase {

    private static final String LOCALHOST = "localhost";
    private static final String GATEWAY_IP = "10.0.0.1";
    private static final int EXPOSED_PORT = 80;
    private static final int BOUND_PORT = 90;

    @Mock
    private Deployment deployment;

    @Mock
    private DeployableContainer<?> deployableContainer;

    @Mock
    private DockerClientExecutor dockerClientExecutor;

    @Mock
    private DockerClient dockerClient;

    @Mock
    private InspectContainerCmd inspectContainerCmd;

    @Mock
    private InspectContainerResponse inspectContainerResponse;

    @Mock
    private HostConfig hostConfig;

    @Mock
    private NetworkSettings networkSettings;

    @Mock
    private ContainerMapping containerMapping;

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

    @Before
    public void setup() {
        Mockito.when(containerMapping.getContainerByName("test")).thenReturn("test");
        Mockito.when(dockerClientExecutor.getDockerClient()).thenReturn(dockerClient);
        Mockito.when(dockerClient.inspectContainerCmd("test")).thenReturn(inspectContainerCmd);
        Mockito.when(inspectContainerCmd.exec()).thenReturn(inspectContainerResponse);

        Mockito.when(inspectContainerResponse.getHostConfig()).thenReturn(hostConfig);
        Mockito.when(inspectContainerResponse.getNetworkSettings()).thenReturn(networkSettings);

        bind(ApplicationScoped.class, ContainerMapping.class, containerMapping);
        bind(ApplicationScoped.class, DockerClientExecutor.class, dockerClientExecutor);
    }

    @Test // equal ip, different ports
    public void shouldUpdateWithPortFromDocker() throws Exception {

        Ports ports = new Ports();
        ports.bind(new ExposedPort("tcp", EXPOSED_PORT), new Binding(BOUND_PORT));
        Mockito.when(networkSettings.getGateway()).thenReturn(LOCALHOST);
        Mockito.when(hostConfig.getPortBindings()).thenReturn(ports);

        bind(ContainerScoped.class,
             Container.class,
             new ContainerImpl("test", deployableContainer, new ContainerDefImpl("arquillian.xml")));

        ProtocolMetaData metadata = new ProtocolMetaData();
        metadata.addContext(new HTTPContext(LOCALHOST, EXPOSED_PORT).add(new Servlet("A", "B")));

        bind(DeploymentScoped.class, ProtocolMetaData.class, metadata);
        fire(metadata);

        ProtocolMetaData updated = getManager().getContext(DeploymentContext.class)
                .getObjectStore().get(ProtocolMetaData.class);

        Assert.assertEquals(BOUND_PORT, updated.getContexts(HTTPContext.class).iterator().next().getPort());
        Assert.assertEquals(LOCALHOST, updated.getContexts(HTTPContext.class).iterator().next().getHost());
        assertEventFired(ProtocolMetaData.class, 1); // twice, but original fire is intercepted and never hit the Counter
    }

    @Test // equal ports, different ip
    public void shouldUpdateWithIPFromDocker() throws Exception {

        Ports ports = new Ports();
        ports.bind(new ExposedPort("tcp", EXPOSED_PORT), new Binding(EXPOSED_PORT));
        Mockito.when(networkSettings.getGateway()).thenReturn(GATEWAY_IP);
        Mockito.when(hostConfig.getPortBindings()).thenReturn(ports);

        bind(ContainerScoped.class,
             Container.class,
             new ContainerImpl("test", deployableContainer, new ContainerDefImpl("arquillian.xml")));

        ProtocolMetaData metadata = new ProtocolMetaData();
        metadata.addContext(new HTTPContext(LOCALHOST, EXPOSED_PORT).add(new Servlet("A", "B")));

        bind(DeploymentScoped.class, ProtocolMetaData.class, metadata);
        fire(metadata);

        ProtocolMetaData updated = getManager().getContext(DeploymentContext.class)
                .getObjectStore().get(ProtocolMetaData.class);

        Assert.assertEquals(EXPOSED_PORT, updated.getContexts(HTTPContext.class).iterator().next().getPort());
        Assert.assertEquals(GATEWAY_IP, updated.getContexts(HTTPContext.class).iterator().next().getHost());
        assertEventFired(ProtocolMetaData.class, 1); // twice, but original fire is intercepted and never hit the Counter
    }

    @Test
    public void shouldNotUpdateIfContainerNotMapped() throws Exception {
        Ports ports = new Ports();
        ports.bind(new ExposedPort("tcp", EXPOSED_PORT), new Binding(EXPOSED_PORT));

        Mockito.when(hostConfig.getPortBindings()).thenReturn(ports);
        Mockito.when(networkSettings.getGateway()).thenReturn(GATEWAY_IP);

        bind(ContainerScoped.class,
             Container.class,
             new ContainerImpl("_UNMAPPED_", deployableContainer, new ContainerDefImpl("arquillian.xml")));

        ProtocolMetaData metadata = new ProtocolMetaData();
        metadata.addContext(new HTTPContext(LOCALHOST, EXPOSED_PORT).add(new Servlet("A", "B")));

        bind(DeploymentScoped.class, ProtocolMetaData.class, metadata);
        fire(metadata);

        ProtocolMetaData updated = getManager().getContext(DeploymentContext.class)
                .getObjectStore().get(ProtocolMetaData.class);

        Assert.assertEquals(metadata.getContexts(HTTPContext.class).iterator().next(), updated.getContexts(HTTPContext.class).iterator().next());
        assertEventFired(ProtocolMetaData.class, 1);
    }
}
