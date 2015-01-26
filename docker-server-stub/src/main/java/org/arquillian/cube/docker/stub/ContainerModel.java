package org.arquillian.cube.docker.stub;

import java.util.HashSet;
import java.util.Set;

public class ContainerModel {

    private String id;
    private Set<String> exposedPorts = new HashSet<>();
    private Set<PortBinding> portBindings = new HashSet<>();
    private Status status;
    
    public ContainerModel(String id) {
        this.id = id;
    }

    public void setExposedPorts(Set<String> exposedPorts) {
        this.exposedPorts = exposedPorts;
    }
    public Set<String> getExposedPorts() {
        return exposedPorts;
    }
    public void setStatus(Status status) {
        this.status = status;
    }
    public Status getStatus() {
        return status;
    }
    public String getId() {
        return id;
    }
    public Set<PortBinding> getPortBindings() {
        return this.portBindings;
    }
    public void addPortBinding(PortBinding portBinding) {
        this.portBindings.add(portBinding);
    }

    static class PortBinding {
        private String exposedPort;
        private Set<String> portBindings = new HashSet<>();
        
        public PortBinding(String exposedPort) {
            this.exposedPort = exposedPort;
        }

        public void addPortBinding(String portBind) {
            portBindings.add(portBind);
        }

        public String getExposedPort() {
            return exposedPort;
        }
        public Set<String> getPortBindings() {
            return portBindings;
        }
    }

    static enum Status {
        CREATED, STARTED, STOPPED, REMOVED;
    }
}
