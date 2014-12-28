package org.arquillian.cube.impl.containerless;

import org.jboss.arquillian.container.spi.ConfigurationException;
import org.jboss.arquillian.container.spi.client.container.ContainerConfiguration;

public class ContainerlessConfiguration implements ContainerConfiguration {

    private String containerlessDocker = "";
    private int embeddedPort = 8080;

    public void setContainerlessDocker(String containerlessDocker) {
        this.containerlessDocker = containerlessDocker;
    }

    public void setEmbeddedPort(int embeddedPort) {
        this.embeddedPort = embeddedPort;
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
