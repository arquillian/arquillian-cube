package org.arquillian.cube.openshift.impl.client.metadata;

import java.net.Inet4Address;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.arquillian.cube.openshift.impl.client.CubeOpenShiftConfiguration;
import org.arquillian.cube.openshift.impl.client.OpenShiftClient;
import org.arquillian.cube.openshift.impl.client.PortForwarder;
import org.arquillian.cube.openshift.impl.client.PortForwarder.PortForwardServer;
import org.arquillian.cube.spi.Binding;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.metadata.HasMappedPorts;
import org.xnio.IoUtils;

public class MappedPorts implements HasMappedPorts {

    private final Cube<?> cube;
    private final OpenShiftClient client;
    private final Map<Integer, Integer> mappedPorts;
    private PortForwarder portForwarder;
    private Map<Integer, PortForwardServer> portForwardServers = new HashMap<Integer, PortForwardServer>();

    public MappedPorts(Cube<?> cube, OpenShiftClient client, CubeOpenShiftConfiguration configuration) {
        this.cube = cube;
        this.client = client;
        this.mappedPorts = new HashMap<Integer, Integer>();
        final String cubeId = cube.getId();
        for (String proxy : configuration.getProxiedContainerPorts()) {
            String[] split = proxy.split(":");
            if (split.length == 2) {
                if (split[0].length() == 0 || cubeId.equals(split[0])) {
                    mappedPorts.put(Integer.valueOf(split[1]), allocateLocalPort());
                }
            }
        }
    }

    @Override
    public synchronized void createProxies() throws Exception {
        if (portForwarder == null) {
            portForwarder = new PortForwarder(client.getClient().getConfiguration(), cube.getId());
        }
        Binding binding = cube.bindings();
        try {
            for (Entry<Integer, Integer> mappedPort : mappedPorts.entrySet()) {
                PortForwardServer server = portForwarder.forwardPort(mappedPort.getValue(), mappedPort.getKey());
                portForwardServers.put(mappedPort.getKey(), server);
                System.out.println(String.format("Forwarding port %s for %s:%d", server.getLocalAddress(), binding.getIP(), mappedPort.getKey()));
            }
        } catch (Throwable t) {
            IoUtils.safeClose(portForwarder);
            portForwarder = null;
            portForwardServers.clear();
            throw t;
        }
    }

    @Override
    public String getIP() {
        // always localhost
        return "localhost";
    }

    @Override
    public Integer forTargetPort(int targetPort) {
        return mappedPorts.get(targetPort);
    }

    @Override
    public synchronized void destroyProxies() {
        if (portForwarder != null) {
            IoUtils.safeClose(portForwarder);
            portForwarder = null;
            portForwardServers.clear();
        }
    }
    
    private int allocateLocalPort() {
        try {
            try (ServerSocket serverSocket = new ServerSocket(0, 0, Inet4Address.getLocalHost())) {
                return serverSocket.getLocalPort();
            }
        } catch (Throwable t) {
            throw new IllegalStateException("Could not allocate local port for forwarding proxy", t);
        }
    }
}