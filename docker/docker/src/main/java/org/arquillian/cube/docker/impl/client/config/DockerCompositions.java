package org.arquillian.cube.docker.impl.client.config;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class DockerCompositions {

    private static final Logger logger = Logger.getLogger(DockerCompositions.class.getName());

    private Map<String, CubeContainer> containers;
    private Map<String, Network> networks;

    public DockerCompositions() {
        this.containers = new LinkedHashMap<>();
        this.networks = new LinkedHashMap<>();
    }

    public Map<String, CubeContainer> getContainers() {
        return containers;
    }

    public void setContainers(Map<String, CubeContainer> containers) {
        this.containers = containers;
    }

    public Map<String, CubeContainer> getNoneManualContainers() {
        Map<String, CubeContainer> autoContainers = new HashMap<>();
        for (Map.Entry<String, CubeContainer> container : containers.entrySet()) {
            if (!container.getValue().isManual()) {
                autoContainers.put(container.getKey(), container.getValue());
            }
        }

        return autoContainers;
    }

    public Map<String, Network> getNetworks() {
        return networks;
    }

    public void setNetworks(Map<String, Network> networks) {
        this.networks = networks;
    }

    public Set<String> getContainerIds() {
        return containers.keySet();
    }

    public Set<String> getNetworkIds() {
        return networks.keySet();
    }

    public CubeContainer get(String id) {
        return containers.get(id);
    }

    public Network getNetwork(String id) {
        return this.networks.get(id);
    }

    public void add(String id, CubeContainer container) {
        this.containers.put(id, container);
    }

    public void add(String id, Network network) {
        this.networks.put(id, network);
    }

    public void merge(DockerCompositions otherContainers) {

        // merge networks
        for (Map.Entry<String, Network> thisNetwork : networks.entrySet()) {
            if (otherContainers.getNetwork(thisNetwork.getKey()) != null) {
                thisNetwork.getValue().merge(otherContainers.getNetwork(thisNetwork.getKey()));
            }
        }
        Map<String, Network> addAllNetworks = new HashMap<>();
        for (Map.Entry<String, Network> otherNetwork : otherContainers.getNetworks().entrySet()) {
            if (getNetwork(otherNetwork.getKey()) == null) {
                addAllNetworks.put(otherNetwork.getKey(), otherNetwork.getValue());
            }
        }
        networks.putAll(addAllNetworks);

        // merge containers
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
     *
     * @param overrideDockerCompositions
     *     that contains information to override.
     */
    public void overrideCubeProperties(DockerCompositions overrideDockerCompositions) {
        final Set<String> containerIds = overrideDockerCompositions.getContainerIds();
        for (String containerId : containerIds) {

            // main definition of containers contains a container that must be overrode
            if (containers.containsKey(containerId)) {
                final CubeContainer cubeContainer = containers.get(containerId);
                final CubeContainer overrideCubeContainer = overrideDockerCompositions.get(containerId);

                cubeContainer.setRemoveVolumes(overrideCubeContainer.getRemoveVolumes());
                
                cubeContainer.setAlwaysPull(overrideCubeContainer.getAlwaysPull());

                if (overrideCubeContainer.hasAwait()) {
                    cubeContainer.setAwait(overrideCubeContainer.getAwait());
                }

                if (overrideCubeContainer.hasBeforeStop()) {
                    cubeContainer.setBeforeStop(overrideCubeContainer.getBeforeStop());
                }

                if (overrideCubeContainer.isManual()) {
                    cubeContainer.setManual(overrideCubeContainer.isManual());
                }

                if (overrideCubeContainer.isKillContainer()) {
                    cubeContainer.setKillContainer(overrideCubeContainer.isKillContainer());
                }
            } else {
                logger.warning(String.format("Overriding Container %s are not defined in main definition of containers.",
                    containerId));
            }
        }
    }
}
