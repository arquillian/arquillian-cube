package org.arquillian.cube.openshift.impl.client.metadata;

import java.util.HashMap;
import java.util.Map;

import org.arquillian.cube.openshift.impl.client.OpenShiftClient;
import org.arquillian.cube.openshift.impl.client.PortForwarder;
import org.arquillian.cube.openshift.impl.client.PortForwarder.PortForwardServer;
import org.arquillian.cube.spi.Binding.PortBinding;
import org.arquillian.cube.spi.metadata.CanForwardPorts;
import org.xnio.IoUtils;

public class ForwardPorts implements CanForwardPorts {

    private final String cubeId;
    private final OpenShiftClient client;
    private PortForwarder portForwarder;
    private Map<Integer, PortForwardServer> portForwardServers = new HashMap<Integer, PortForwardServer>();

    public ForwardPorts(String cubeId, OpenShiftClient client) {
        this.cubeId = cubeId;
        this.client = client;
    }

    @Override
    public synchronized void createProxy(PortBinding binding) throws Exception {
        if (portForwarder == null) {
            portForwarder = new PortForwarder(client.getClient().getConfiguration(), cubeId);
        }
        try {
            final boolean assignedPort = binding.getExposedPort() == null;
            PortForwardServer server = portForwarder.forwardPort(assignedPort ? 0 : binding.getExposedPort(), binding.getBindingPort());
            portForwardServers.put(binding.getBindingPort(), server);
            System.out.println(String.format("Forwarding port %s for %s:%d", server.getLocalAddress(), binding.getParent().getIP(), binding.getBindingPort()));
        } catch (Exception e) {
            IoUtils.safeClose(portForwarder);
            portForwarder = null;
            portForwardServers.clear();
            throw e;
        }
    }

    @Override
    public Integer getForwardedPort(PortBinding binding) {
        PortForwardServer server = portForwardServers.get(binding.getBindingPort());
        return server == null ? null : server.getLocalAddress().getPort();
    }

    @Override
    public synchronized void destroyProxies() {
        if (portForwarder != null) {
            IoUtils.safeClose(portForwarder);
            portForwarder = null;
            portForwardServers.clear();
        }
    }
}