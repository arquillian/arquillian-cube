package org.arquillian.cube.impl.container;

import org.arquillian.cube.spi.event.CubeControlEvent;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;

public class DockerDeployableContainer implements DeployableContainer<DockerDeployableContainerConfiguration> {

    private DockerDeployableContainerConfiguration configuration;
    
    @Override
    public Class<DockerDeployableContainerConfiguration> getConfigurationClass() {
        return DockerDeployableContainerConfiguration.class;
    }

    @Override
    public void setup(DockerDeployableContainerConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void start() throws LifecycleException {
        //In this case we need to start the configured docker container on container section.
        //We need to use the this.coniguration instance to do it. 
        //To create and because we have a dependency to docker module we can do sth like:
        //@Inject
        //private Event<CubeControlEvent> controlEvent;
        //and throw the create and start event. The problem is that we need to create/modify a cuberegistry with the docker image defined in container section 
        //and not in the extension section. And because CubeRegistry is shared then the same container will be started by container logic and by extension logic.
        //Another option is to use DockerClientExecutor directly so container created by this module and containers created by the extensions are valid and independent.
    }

    @Override
    public void stop() throws LifecycleException {
        // TODO Auto-generated method stub

    }

    @Override
    public ProtocolDescription getDefaultProtocol() {
        //I think it has no sense to have this because the application will be a self packaged jar
        return new ProtocolDescription("Servlet 3.0");
    }

    @Override
    public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void undeploy(Archive<?> archive) throws DeploymentException {
    }

    @Override
    public void deploy(Descriptor descriptor) throws DeploymentException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void undeploy(Descriptor descriptor) throws DeploymentException {
        throw new UnsupportedOperationException("Not implemented");
    }

}
