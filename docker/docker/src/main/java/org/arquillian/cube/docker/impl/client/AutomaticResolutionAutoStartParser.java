
package org.arquillian.cube.docker.impl.client;

import org.arquillian.cube.docker.impl.util.AutoStartOrderUtil;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AutomaticResolutionAutoStartParser implements AutoStartParser {

    private List<String> deployableContainers;
    private Map<String, Object> containerDefinition;

    public AutomaticResolutionAutoStartParser(List<String> deployableContainers, Map<String, Object> containerDefinitions) {
        this.deployableContainers = deployableContainers;
        this.containerDefinition = containerDefinitions;
    }

    @Override
    public Map<String, AutoStartOrderUtil.Node> parse() {

        Map<String, AutoStartOrderUtil.Node> nodes = new HashMap<>();

        for(String deployableContainer : this.deployableContainers) {
            Map<String, Object> content = (Map<String, Object>) containerDefinition.get(deployableContainer);
            if (content == null) {
                return nodes;
            }

            if (content.containsKey("links")) {
                Collection<String> links = (Collection<String>) content.get("links");
                for (String link : links) {
                    String[] parsed = link.split(":");
                    String name = parsed[0];

                    if (containerDefinition.containsKey(name)) {
                        AutoStartOrderUtil.Node child = AutoStartOrderUtil.Node.from(name);
                        nodes.put(name, child);
                    }
                }
            }
        }

        return nodes;
    }

}
