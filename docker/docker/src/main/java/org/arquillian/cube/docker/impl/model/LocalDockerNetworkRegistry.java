package org.arquillian.cube.docker.impl.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.arquillian.cube.docker.impl.client.config.Network;

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

    @Override
    public Network getNetwork(String id) {
        return networks.get(id);
    }

    @Override
    public List<Network> getNetworks() {
        List<Network> cubeList = new ArrayList<>(this.networks.values());
        return Collections.unmodifiableList(cubeList);
    }
}
