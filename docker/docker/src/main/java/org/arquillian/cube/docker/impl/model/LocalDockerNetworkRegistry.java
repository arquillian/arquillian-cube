package org.arquillian.cube.docker.impl.model;

import org.arquillian.cube.docker.impl.client.config.Network;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class LocalDockerNetworkRegistry implements NetworkRegistry {

    private Map<String, Network> networks;

    public LocalDockerNetworkRegistry() {
        super();
        this.networks = new HashMap<>();
    }

    @Override
    public void addNetwork(String id, Network network) {
        this.networks.put(id, network);
    }

    @Override
    public Set<String> getNetworkIds() {
        return Collections.unmodifiableSet(this.networks.keySet());
    }

    @Override
    public void removeNetwork(String id) {
        this.networks.remove(id);
    }
}
