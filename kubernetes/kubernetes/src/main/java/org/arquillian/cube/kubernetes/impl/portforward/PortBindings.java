package org.arquillian.cube.kubernetes.impl.portforward;

import java.net.Inet4Address;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.arquillian.cube.kubernetes.api.Configuration;
import org.arquillian.cube.kubernetes.impl.portforward.PortForwarder.PortForwardServer;
import org.arquillian.cube.spi.metadata.HasPortBindings;
import org.xnio.IoUtils;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;

public class PortBindings implements HasPortBindings {

    private final Pod resource;
    private final String id;
    private final KubernetesClient client;

    private final Map<Integer, Integer> proxiedPorts;
    private final Map<Integer, PortAddress> mappedPorts;
    private final Set<Integer> containerPorts;
    private PortForwarder portForwarder;
    private Map<Integer, PortForwardServer> portForwardServers = new HashMap<Integer, PortForwardServer>();

    public PortBindings(Pod resource, KubernetesClient client, Configuration configuration) {
        this.resource = resource;
        this.client = client;
        this.id = resource.getMetadata().getName();

        this.mappedPorts = new HashMap<Integer, PortAddress>();
        this.proxiedPorts = new LinkedHashMap<Integer, Integer>();
        for (String proxy : configuration.getPodForwardPorts()) {
            // Syntax: pod:containerPort - backward compatibility, uses allocated port
            // pod:mappedPort:containerPort - use the same port than container port
            // pod::containerPort - mappedPort == containerPort (same as oc port-forward), except mappedPort may be 0, which will dynamically allocate a port
            String[] split = proxy.trim().split(":");
            final String containerName = split[0];
            if (containerName.length() > 0 && !id.equals(containerName)) {
                // empty matches all, so if it's not empty and doesn't match, skip it
                continue;
            }
            final Integer containerPort;
            final Integer mappedPort;
            if (split.length == 2) {
                // backward compatibility: pod:containerPort
                // allocate mappedPort
                containerPort = Integer.valueOf(split[1]);
                mappedPort = allocateLocalPort(0);
            } else if (split.length == 3) {
                containerPort = Integer.valueOf(split[2]);
                if (split[1].length() == 0) {
                    // pod::containerPort - use the same port as container port
                    mappedPort = allocateLocalPort(containerPort);
                } else {
                    //pod:mappedPort:containerPort - use specified port; if 0, we'll allocate one
                    mappedPort = allocateLocalPort(Integer.valueOf(split[1]));
                }
            } else {
                // log an error
                continue;
            }
            proxiedPorts.put(containerPort, mappedPort);
            mappedPorts.put(containerPort, new PortAddressImpl("localhost", mappedPort));
        }

        this.containerPorts = new LinkedHashSet<Integer>();
        for (Container container : resource.getSpec().getContainers()) {
            for (ContainerPort containerPort : container.getPorts()) {
                if (containerPort.getContainerPort() == null) {
                    continue;
                }
                final int port = containerPort.getContainerPort();
                containerPorts.add(port);
                if (!proxiedPorts.containsKey(port)) {
                    final Integer hostPort = containerPort.getHostPort();
                    if (hostPort != null) {
                        // we don't care about hostIP at the moment
                        mappedPorts.put(port, new PortAddressImpl(containerPort.getHostIP(), hostPort));
                    }
                }
            }
        }

        // add proxied ports into the mix, if they're not already there
        containerPorts.addAll(proxiedPorts.keySet());
    }

    @Override
    public boolean isBound() {
        return true;
    }

    @Override
    public String getContainerIP() {
        if (isBound() && resource.getStatus() != null) {
            return resource.getStatus().getPodIP();
        }
        return null;
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
    public Set<Integer> getBoundPorts() {
        // no difference between these
        return Collections.unmodifiableSet(containerPorts);
    }

    @Override
    public synchronized PortAddress getMappedAddress(int targetPort) {
        if (mappedPorts.containsKey(targetPort)) {
            return mappedPorts.get(targetPort);
        }
        return null;
    }

    public synchronized void podStarted() throws Exception {
        if (resource != null && resource.getSpec() != null
                && resource.getSpec().getContainers() != null) {
            for (Container container : resource.getSpec().getContainers()) {
                for (ContainerPort containerPort : container.getPorts()) {
                    if (containerPort.getContainerPort() == null) {
                        continue;
                    }
                    final int port = containerPort.getContainerPort();
                    if (!proxiedPorts.containsKey(port)) {
                        final Integer hostPort = containerPort.getHostPort();
                        if (hostPort != null) {
                            // overwrite as hostIP info may have changed
                            mappedPorts.put(port, new PortAddressImpl(containerPort.getHostIP(), hostPort));
                        }
                    }
                }
            }
        }

        createProxies();
    }

    private void createProxies() throws Exception {
        if (proxiedPorts.isEmpty()) {
            return;
        }
        if (portForwarder == null) {
            portForwarder = new PortForwarder(client.getConfiguration(), id);
        }
        try {
            for (Entry<Integer, Integer> mappedPort : proxiedPorts.entrySet()) {
                PortForwardServer server = portForwarder.forwardPort(mappedPort.getValue(), mappedPort.getKey());
                portForwardServers.put(mappedPort.getKey(), server);
                System.out.println(String.format("Forwarding port %s for %s:%d", server.getLocalAddress(),
                        getContainerIP(), mappedPort.getKey()));
            }
        } catch (Throwable t) {
            IoUtils.safeClose(portForwarder);
            portForwarder = null;
            portForwardServers.clear();
            throw t;
        }
    }

    public synchronized void podStopped() {
        destroyProxies();
    }

    private void destroyProxies() {
        if (portForwarder != null) {
            IoUtils.safeClose(portForwarder);
            portForwarder = null;
            portForwardServers.clear();
        }
    }

    //If you are going to change this method, please also change the tests PortForwarderPort.java
    private int allocateLocalPort(int port) {
        try {
            try (ServerSocket serverSocket = new ServerSocket(port, 0, Inet4Address.getLocalHost())) {
                return serverSocket.getLocalPort();
            }
        } catch (Throwable t) {
            throw new IllegalStateException("Could not allocate local port for forwarding proxy", t);
        }
    }
}
