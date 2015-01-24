package org.arquillian.cube.spi;

import java.util.HashSet;
import java.util.Set;

public class Binding {

    private String ip;

    public Set<PortBinding> bindings;

    public Binding(String ip) {
        this.ip = ip;
        this.bindings = new HashSet<PortBinding>();
    }

    public String getIP() {
        return ip;
    }

    public Set<PortBinding> getPortBindings() {
        return bindings;
    }

    public void addPortBinding(Integer exposedPort, Integer bindingPort) {
        this.bindings.add(new PortBinding(exposedPort, bindingPort));
    }

    public boolean arePortBindings() {
        return !this.bindings.isEmpty();
    }

    public PortBinding getBindingForExposedPort(Integer exposedPort) {
        for(PortBinding binding : this.bindings) {
            if(binding.getExposedPort().equals(exposedPort)) {
                return binding;
            }
        }
        return null;
    }

    public static class PortBinding {
        private Integer exposedPort;
        private Integer bindingPort;

        public PortBinding(Integer exposedPort, Integer bindingPort) {
            this.exposedPort = exposedPort;
            this.bindingPort = bindingPort;
        }

        public Integer getExposedPort() {
            return exposedPort;
        }

        public Integer getBindingPort() {
            return bindingPort;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result
                    + ((bindingPort == null) ? 0 : bindingPort.hashCode());
            result = prime * result
                    + ((exposedPort == null) ? 0 : exposedPort.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            PortBinding other = (PortBinding) obj;
            if (bindingPort == null) {
                if (other.bindingPort != null)
                    return false;
            } else if (!bindingPort.equals(other.bindingPort))
                return false;
            if (exposedPort == null) {
                if (other.exposedPort != null)
                    return false;
            } else if (!exposedPort.equals(other.exposedPort))
                return false;
            return true;
        }
    }
}
