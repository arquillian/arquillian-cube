package org.arquillian.cube.impl.util;

import java.net.InetAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.arquillian.cube.spi.Binding;
import org.arquillian.cube.spi.Binding.PortBinding;
import org.arquillian.cube.spi.metadata.HasPortBindings;

public class TestPortBindings implements HasPortBindings {

    private final Map<Integer, PortAddress> mappedPorts;
    private final Set<Integer> containerPorts;
    private final Set<Integer> boundPorts;
    private String containerIP;

    public TestPortBindings(Binding configuredBindings) {
        this.mappedPorts = new HashMap<Integer, PortAddress>();
        this.containerPorts = new LinkedHashSet<Integer>();
        containerIP = configuredBindings.getIP();
        for (PortBinding portBinding : configuredBindings.getPortBindings()) {
            final int exposedPort = portBinding.getExposedPort();
            final Integer boundPort = portBinding.getBindingPort();
            containerPorts.add(exposedPort);
            if (boundPort != null && containerIP != null) {
                mappedPorts.put(exposedPort, new PortAddressImpl(configuredBindings.getIP(), boundPort));
            }
        }
        this.boundPorts = new LinkedHashSet<Integer>(containerPorts.size());
    }

    @Override
    public boolean isBound() {
        return true;
    }

    @Override
    public synchronized String getContainerIP() {
        return containerIP;
    }

    @Override
    public String getInternalIP() {
        return null;
    }

    @Override
    public Set<Integer> getContainerPorts() {
        return Collections.unmodifiableSet(containerPorts);
    }

    @Override
    public synchronized Set<Integer> getBoundPorts() {
        return isBound() ? Collections.unmodifiableSet(boundPorts) : getContainerPorts();
    }

    @Override
    public synchronized PortAddress getMappedAddress(int targetPort) {
        if (mappedPorts.containsKey(targetPort)) {
            return mappedPorts.get(targetPort);
        }
        return null;
    }

    @Override
    public InetAddress getPortForwardBindAddress() {
        return null;
    }
}
