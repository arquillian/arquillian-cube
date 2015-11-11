package org.arquillian.cube.impl.containerless;

import org.jboss.arquillian.container.spi.ConfigurationException;
import org.jboss.arquillian.container.spi.client.container.ContainerConfiguration;

public class ContainerlessConfiguration implements ContainerConfiguration {

    private String containerlessDocker = null;
    private int embeddedPort = 0;

    public void setContainerlessDocker(String containerlessDocker) {
        this.containerlessDocker = containerlessDocker;
    }

    public boolean isContainerlessDockerSet() {
        return this.containerlessDocker != null;
    }

    public void setEmbeddedPort(int embeddedPort) {
        this.embeddedPort = embeddedPort;
    }

    public boolean isEmbeddedPortSet() {
        return this.embeddedPort > 0;
    }

    public int getEmbeddedPort() {
        return embeddedPort;
    }

    public String getContainerlessDocker() {
        return containerlessDocker;
    }

    @Override
    public void validate() throws ConfigurationException {
    }

}
