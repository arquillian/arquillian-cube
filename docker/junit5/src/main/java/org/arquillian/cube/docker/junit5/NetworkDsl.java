package org.arquillian.cube.docker.junit5;

import org.arquillian.cube.docker.impl.client.containerobject.dsl.Network;
import org.arquillian.cube.docker.impl.client.containerobject.dsl.NetworkBuilder;

public class NetworkDsl {

    private NetworkBuilder networkBuilder;
    private Network network;

    public NetworkDsl(String networkId) {
        this.networkBuilder = Network.withDefaultDriver(networkId);
    }

    public NetworkDsl(String networkName, String driver) {
        this.networkBuilder = Network.withDriver(networkName, driver);
    }

    public String getNetworkName() {
        return this.network.getId();
    }

    public Network buildNetwork() {
        this.network = this.networkBuilder.build();
        return this.network;
    }

}
