package org.arquillian.cube.client;

import java.util.HashMap;
import java.util.Map;

public class ContainerMapping {

    private Map<String, String> containers = new HashMap<String, String>();
    
    public void addContainer(String containerName, String containerId) {
        this.containers.put(containerName, containerId);
    }
    
    public String removeContainer(String containerName) {
        return this.containers.remove(containerName);
    }

    public String getContainerByName(String containerName) {
        return this.containers.get(containerName);
    }
    
    public String getDefaultContainer() {
        if(this.containers.size() == 1) {
            return this.containers.values().iterator().next();
        } else {
            throw new IllegalArgumentException("More than one container configured.");
        }
    }
    
}
