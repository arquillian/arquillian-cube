package org.arquillian.cube.docker.impl.model;

import org.arquillian.cube.docker.impl.client.config.Network;

import java.util.Set;

public interface NetworkRegistry {

    // TODO in case of generalizing this we should create an interface of typeNetwork instead of reusing this one.
    void addNetwork(String id, Network network);
    Set<String> getNetworkIds();
    void removeNetwork(String id);

}
