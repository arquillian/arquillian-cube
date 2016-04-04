
package org.arquillian.cube.docker.impl.client;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.arquillian.cube.docker.impl.client.config.CubeContainer;
import org.arquillian.cube.docker.impl.client.config.CubeContainers;
import org.arquillian.cube.docker.impl.client.config.Link;
import org.arquillian.cube.docker.impl.util.AutoStartOrderUtil;

public class AutomaticResolutionAutoStartParser implements AutoStartParser {

    private List<String> deployableContainers;
    private CubeContainers containerDefinition;

    public AutomaticResolutionAutoStartParser(List<String> deployableContainers, CubeContainers containerDefinitions) {
        this.deployableContainers = deployableContainers;
        this.containerDefinition = containerDefinitions;
    }

    @Override
    public Map<String, AutoStartOrderUtil.Node> parse() {

        Map<String, AutoStartOrderUtil.Node> nodes = new HashMap<>();

        for(String deployableContainer : this.deployableContainers) {
            CubeContainer content = containerDefinition.get(deployableContainer);
            if (content == null) {
                return nodes;
            }

            if (content.getLinks() != null) {
                Collection<Link> links = content.getLinks();
                for (Link link : links) {
                    String name = link.getName();

                    if (containerDefinition.get(name) != null) {
                        AutoStartOrderUtil.Node child = AutoStartOrderUtil.Node.from(name);
                        nodes.put(name, child);
                    }
                }
            }
        }

        return nodes;
    }

    @Override
    public String toString() {
        return AutoStartOrderUtil.toString(parse());
    }
}
