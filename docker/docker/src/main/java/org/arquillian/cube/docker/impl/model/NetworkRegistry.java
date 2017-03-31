package org.arquillian.cube.docker.impl.model;

import java.util.List;
import java.util.Set;
import org.arquillian.cube.docker.impl.client.config.Network;

public interface NetworkRegistry {

    // TODO in case of generalizing this we should create an interface of typeNetwork instead of reusing this one.
    void addNetwork(String id, Network network);

    Set<String> getNetworkIds();

    void removeNetwork(String id);

    Network getNetwork(String id);

    List<Network> getNetworks();
}
