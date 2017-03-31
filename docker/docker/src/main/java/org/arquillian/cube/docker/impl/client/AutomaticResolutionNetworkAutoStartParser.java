package org.arquillian.cube.docker.impl.client;

import org.arquillian.cube.docker.impl.client.config.CubeContainer;
import org.arquillian.cube.docker.impl.client.config.DockerCompositions;
import org.arquillian.cube.docker.impl.util.AutoStartOrderUtil;
import org.arquillian.cube.spi.AutoStartParser;
import org.arquillian.cube.spi.Node;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AutomaticResolutionNetworkAutoStartParser implements AutoStartParser {

    private List<String> deployableContainers;
    private DockerCompositions containerDefinition;

    public AutomaticResolutionNetworkAutoStartParser(List<String> deployableContainers, DockerCompositions containerDefinitions) {
        this.deployableContainers = deployableContainers;
        this.containerDefinition = containerDefinitions;
    }

    @Override
    public Map<String, Node> parse() {

        Map<String, Node> nodes = new HashMap<>();

        Set<String> networksIds = this.containerDefinition.getNetworkIds();

        for (Map.Entry<String, CubeContainer> container : this.containerDefinition.getNoneManualContainers().entrySet()) {
            // If cube is not named as the deployable container (this is done in this way because containers that are named as the deployable container are started by another process).
            if (! this.deployableContainers.contains(container.getKey())) {

                final String networkMode = container.getValue().getNetworkMode();
                if (networkMode != null) {
                    // if the network that this cube must connect is registered in current cube definition
                    if (networksIds.contains(networkMode)) {
                        String name = container.getKey();
                        Node child = Node.from(name);
                        nodes.put(name, child);
                    }
                }
            }
        }

        return nodes;
    }
}
