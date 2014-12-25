package org.arquillian.cube.impl.containerless;

import org.jboss.arquillian.container.spi.ConfigurationException;
import org.jboss.arquillian.container.spi.client.container.ContainerConfiguration;

public class ContainerlessConfiguration implements ContainerConfiguration {

    private String containerlessDocker = "";

    public void setContainerlessDocker(String containerlessDocker) {
        this.containerlessDocker = containerlessDocker;
    }

    public String getContainerlessDocker() {
        return containerlessDocker;
    }

    @Override
    public void validate() throws ConfigurationException {
    }

}
