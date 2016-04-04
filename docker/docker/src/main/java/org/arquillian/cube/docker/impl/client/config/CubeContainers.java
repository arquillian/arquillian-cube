package org.arquillian.cube.docker.impl.client.config;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class CubeContainers {

    private static final Logger logger = Logger.getLogger(CubeContainers.class.getName());

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

    /**
     * This method only overrides properties that are specific from Cube like await strategy or before stop events.
     * @param overrideCubeContainers that contains information to override.
     */
    public void overrideCubeProperties(CubeContainers overrideCubeContainers) {
        final Set<String> containerIds = overrideCubeContainers.getContainerIds();
        for (String containerId : containerIds) {

            // main definition of containers contains a container that must be overrode
            if (containers.containsKey(containerId)) {
                final CubeContainer cubeContainer = containers.get(containerId);
                final CubeContainer overrideCubeContainer = overrideCubeContainers.get(containerId);

                if (overrideCubeContainer.hasAwait()) {
                    cubeContainer.setAwait(overrideCubeContainer.getAwait());
                }

                if (overrideCubeContainer.hasBeforeStop()) {
                    cubeContainer.setBeforeStop(overrideCubeContainer.getBeforeStop());
                }
            } else {
                logger.warning(String.format("Overriding Container %s are not defined in main definition of containers."));
            }
        }
    }

}
