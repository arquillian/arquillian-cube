package org.arquillian.cube.docker.impl.client.config;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class CubeContainers {

    private Map<String, CubeContainer> containers;

    public CubeContainers() {
        this.containers = new LinkedHashMap<String, CubeContainer>();
    }

    public Map<String, CubeContainer> getContainers() {
        return containers;
    }

    public Set<String> getContainerIds() {
        return containers.keySet();
    }

    public void setContainers(Map<String, CubeContainer> containers) {
        this.containers = containers;
    }

    public CubeContainer get(String id) {
        return containers.get(id);
    }

    public void add(String id, CubeContainer container) {
        this.containers.put(id, container);
    }

    public void merge(CubeContainers otherContainers) {
        for (Map.Entry<String, CubeContainer> thisContainer : containers.entrySet()) {
            if (otherContainers.get(thisContainer.getKey()) != null) {
                thisContainer.getValue().merge(otherContainers.get(thisContainer.getKey()));
            }
        }
        Map<String, CubeContainer> addAll = new HashMap<String, CubeContainer>();
        for (Map.Entry<String, CubeContainer> otherContainer : otherContainers.getContainers().entrySet()) {
            if (get(otherContainer.getKey()) == null) {
                addAll.put(otherContainer.getKey(), otherContainer.getValue());
            }
        }
        containers.putAll(addAll);
    }
}
