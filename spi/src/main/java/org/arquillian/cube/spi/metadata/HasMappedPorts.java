package org.arquillian.cube.spi.metadata;


public interface HasMappedPorts extends CubeMetadata {
    
    String getIP();

    Integer forTargetPort(int targetPort);

    void createProxies() throws Exception;

    void destroyProxies();
}
