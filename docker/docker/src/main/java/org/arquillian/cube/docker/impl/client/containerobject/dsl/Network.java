package org.arquillian.cube.docker.impl.client.containerobject.dsl;

/**
 * Base class representing a network.
 */
public class Network {

    private String id;
    private org.arquillian.cube.docker.impl.client.config.Network network;

    protected Network(String id, org.arquillian.cube.docker.impl.client.config.Network network) {
        this.id = id;
        this.network = network;
    }

    public static NetworkBuilder withDefaultDriver(String networkName) {
        return NetworkBuilder.withDefaultDriver(networkName);
    }

    public static NetworkBuilder withDriver(String networkName, String driver) {
        return NetworkBuilder.withDriver(networkName, driver);
    }

    public String getId() {
        return id;
    }

    public org.arquillian.cube.docker.impl.client.config.Network getNetwork() {
        return network;
    }
}
