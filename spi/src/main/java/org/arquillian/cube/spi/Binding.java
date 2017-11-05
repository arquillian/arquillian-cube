package org.arquillian.cube.spi;

import java.util.HashSet;
import java.util.Set;

public class Binding {

    public Set<PortBinding> bindings;
    private String ip;
    private String internalIp;

    public Binding(String ip) {
        this(ip, null);
    }

    public Binding(String ip, String internalIp) {
        this.ip = ip;
        this.internalIp = internalIp;
        this.bindings = new HashSet<PortBinding>();
    }

    public String getIP() {
        return ip;
    }

    public Set<PortBinding> getPortBindings() {
        return new HashSet<PortBinding>(bindings);
    }

    public int getNumberOfPortBindings() {
        return this.bindings.size();
    }

    public PortBinding getFirstPortBinding() {
        for (PortBinding binding : this.bindings) {
            return binding;
        }

        return null;
    }

    public String getInternalIP() {
        return internalIp;
    }

    /**
     * @param exposedPort
     *     the port exposed by the container (e.g. EXPOSE or Pod.Spec.Container[].Ports[])
     * @param bindingPort
     *     the port to which the container port is bound on this IP (e.g. docker run -p ip:hostPort:exposedPort or
     *     Pod.Spec.Container[].Ports[].HostPort)
     */
    public Binding addPortBinding(Integer exposedPort, Integer bindingPort) {
        this.bindings.add(new PortBinding(exposedPort, bindingPort));
        return this;
    }

    public boolean arePortBindings() {
        return !this.bindings.isEmpty();
    }

    public PortBinding getBindingForExposedPort(Integer exposedPort) {
        for (PortBinding binding : this.bindings) {
            if (exposedPort.equals(binding.getExposedPort())) {
                return binding;
            }
        }
        return null;
    }

    public class PortBinding {
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

        public Binding getParent() {
            return Binding.this;
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
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            PortBinding other = (PortBinding) obj;
            if (bindingPort == null) {
                if (other.bindingPort != null) {
                    return false;
                }
            } else if (!bindingPort.equals(other.bindingPort)) {
                return false;
            }
            if (exposedPort == null) {
                if (other.exposedPort != null) {
                    return false;
                }
            } else if (!exposedPort.equals(other.exposedPort)) {
                return false;
            }
            return true;
        }
    }
}
