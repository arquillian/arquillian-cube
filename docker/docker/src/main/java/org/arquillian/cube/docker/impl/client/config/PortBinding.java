package org.arquillian.cube.docker.impl.client.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PortBinding {
    private String host;
    private int bound;
    private ExposedPort exposed;

    public PortBinding(String host, int bound, ExposedPort exposed) {
        super();
        this.host = host;
        this.bound = bound;
        this.exposed = exposed;
    }

    public static PortBinding valueOf(String portBinding) {
        ExposedPort exposed;
        int bound;
        String host = null;

        String[] elements = portBinding.split("->");
        if (elements.length == 1) {
            // exposed port is only set and same port will be used as port binding.
            int positionOfProtocolSeparator = elements[0].indexOf("/");
            String bindingPortValue = elements[0];
            if (positionOfProtocolSeparator > -1) {
                //means that the protocol part is also set.
                bindingPortValue = elements[0].substring(0, positionOfProtocolSeparator);
            }
            exposed = ExposedPort.valueOf(elements[0]);
            bound = exposed.getExposed();

            if (bindingPortValue.indexOf(":") != -1) {
                host = bindingPortValue.substring(0, bindingPortValue.lastIndexOf(":"));
            }
        } else if (elements.length == 2) {
            exposed = ExposedPort.valueOf(elements[1]);
            if (elements[0].indexOf(":") != -1) {
                host = elements[0].substring(0, elements[0].lastIndexOf(":"));
                bound = Integer.parseInt(elements[0].substring(elements[0].lastIndexOf(":") + 1, elements[0].length()));
            } else {
                bound = Integer.parseInt(elements[0]);
            }
        } else {
            throw new IllegalArgumentException("Could not create PortBinding from " + portBinding);
        }
        return new PortBinding(host, bound, exposed);
    }

    public static Collection<PortBinding> valuesOf(Collection<String> bindings) {
        if (bindings == null) {
            return null;
        }
        List<PortBinding> result = new ArrayList<PortBinding>();
        for (String binding : bindings) {
            result.add(valueOf(binding));
        }
        return result;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getBound() {
        return bound;
    }

    public void setBound(int bound) {
        this.bound = bound;
    }

    public ExposedPort getExposedPort() {
        return exposed;
    }

    public void setExposedPort(ExposedPort exposed) {
        this.exposed = exposed;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + bound;
        result = prime * result + ((exposed == null) ? 0 : exposed.hashCode());
        result = prime * result + ((host == null) ? 0 : host.hashCode());
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
        if (bound != other.bound) {
            return false;
        }
        if (exposed == null) {
            if (other.exposed != null) {
                return false;
            }
        } else if (!exposed.equals(other.exposed)) {
            return false;
        }
        if (host == null) {
            if (other.host != null) {
                return false;
            }
        } else if (!host.equals(other.host)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        if (host != null) {
            sb.append(host).append(":");
        }
        if (exposed.getExposed() != bound) {
            sb.append(bound).append("->");
        }
        sb.append(exposed.getExposed()).append("/").append(exposed.getType());

        return sb.toString();
    }
}
