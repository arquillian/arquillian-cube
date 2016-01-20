package org.arquillian.cube.spi.metadata;

import org.arquillian.cube.spi.Binding.PortBinding;

public interface CanForwardPorts extends CubeMetadata {

    void createProxy(PortBinding binding) throws Exception;

    public Integer getForwardedPort(PortBinding binding);

    void destroyProxies();
}
