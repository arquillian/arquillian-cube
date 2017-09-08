package org.arquillian.cube.openshift.impl.model;

import io.fabric8.kubernetes.api.model.v2_6.Container;
import io.fabric8.kubernetes.api.model.v2_6.ContainerPort;
import io.fabric8.kubernetes.api.model.v2_6.Pod;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import org.arquillian.cube.kubernetes.impl.portforward.PortForwarder;
import org.arquillian.cube.openshift.impl.client.CubeOpenShiftConfiguration;
import org.arquillian.cube.openshift.impl.client.OpenShiftClient;
import org.arquillian.cube.openshift.impl.client.OpenShiftClient.ResourceHolder;
import org.arquillian.cube.openshift.impl.client.metadata.CopyFromContainer;
import org.arquillian.cube.openshift.impl.dns.ArqCubeNameService;
import org.arquillian.cube.spi.BaseCube;
import org.arquillian.cube.spi.Binding;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeControlException;
import org.arquillian.cube.spi.event.lifecycle.AfterCreate;
import org.arquillian.cube.spi.event.lifecycle.AfterDestroy;
import org.arquillian.cube.spi.event.lifecycle.AfterStart;
import org.arquillian.cube.spi.event.lifecycle.AfterStop;
import org.arquillian.cube.spi.event.lifecycle.BeforeCreate;
import org.arquillian.cube.spi.event.lifecycle.BeforeDestroy;
import org.arquillian.cube.spi.event.lifecycle.BeforeStart;
import org.arquillian.cube.spi.event.lifecycle.BeforeStop;
import org.arquillian.cube.spi.event.lifecycle.CubeLifecyleEvent;
import org.arquillian.cube.spi.metadata.CanCopyFromContainer;
import org.arquillian.cube.spi.metadata.HasPortBindings;
import org.arquillian.cube.spi.metadata.IsBuildable;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.xnio.IoUtils;

import static org.arquillian.cube.openshift.impl.client.ResourceUtil.isRunning;
import static org.arquillian.cube.openshift.impl.client.ResourceUtil.toBinding;

public class BuildablePodCube extends BaseCube<Void> {

    private String id;
    private Pod resource;
    private Template<Pod> template;
    private Cube.State state;
    private CubeOpenShiftConfiguration configuration;
    private OpenShiftClient client;

    private PortBindings portBindings;
    private ResourceHolder holder;

    @Inject
    private Event<CubeLifecyleEvent> lifecycle;

    private static final Logger logger = Logger.getLogger(BuildablePodCube.class.getName());

    public BuildablePodCube(Pod resource, OpenShiftClient client, CubeOpenShiftConfiguration configuration) {
        this.id = resource.getMetadata().getName();
        this.resource = resource;
        this.template = new Template.PodTemplate(resource);
        this.client = client;
        this.configuration = configuration;
        this.portBindings = new PortBindings();
        addDefaultMetadata();
    }

    private void addDefaultMetadata() {
        if (template.getRefs() != null && template.getRefs().size() > 0) {
            addMetadata(IsBuildable.class, new IsBuildable(template.getRefs().get(0).getPath()));
        }
        addMetadata(HasPortBindings.class, this.portBindings);
        addMetadata(CanCopyFromContainer.class, new CopyFromContainer(getId(), client));
    }

    @Override
    public State state() {
        return state;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void create() throws CubeControlException {
        try {
            lifecycle.fire(new BeforeCreate(id));
            holder = client.build(template);
            this.state = State.CREATED;
            lifecycle.fire(new AfterCreate(id));
        } catch (Exception e) {
            this.state = State.CREATE_FAILED;
            throw CubeControlException.failedCreate(getId(), e);
        }
    }

    @Override
    public void start() throws CubeControlException {
        try {
            lifecycle.fire(new BeforeStart(id));
            holder.setPod(client.createAndWait(holder.getPod()));
            this.state = State.STARTED;
            try {
                portBindings.podStarted();
            } catch (Exception e) {
                try {
                    destroyPod(holder.getPod());
                } catch (Exception e1) {
                }
                throw e;
            }
            lifecycle.fire(new AfterStart(id));

            // Add the routes to JVM's name service.
            ArqCubeNameService.setRoutes(client.getClientExt().routes().list(), this.configuration.getRouterHost());
        } catch (Exception e) {
            this.state = State.START_FAILED;
            throw CubeControlException.failedStart(getId(), e);
        }
    }

    private void destroyPod(Pod pod) throws Exception {
        if (configuration.isNamespaceCleanupEnabled()) {
            client.destroy(pod);
        } else {
            logger.info("Ignoring cleanup for pod " + pod.getMetadata().getName());
        }
    }

    @Override
    public void stop() throws CubeControlException {
        try {
            lifecycle.fire(new BeforeStop(id));
            destroyPod(holder.getPod());
            try {
                portBindings.podStopped();
            } catch (Exception e) {
                // this shouldn't prevent normal shutdown behavior
            }
            this.state = State.STOPPED;
            lifecycle.fire(new AfterStop(id));
        } catch (Exception e) {
            this.state = State.STOP_FAILED;
            throw CubeControlException.failedStop(getId(), e);
        }
    }

    @Override
    public void destroy() throws CubeControlException {
        try {
            lifecycle.fire(new BeforeDestroy(id));
            List<Exception> exceptions = client.clean(holder);
            if (exceptions.size() > 0) {
                throw exceptions.get(0);
            }
            this.state = State.DESTROYED;
            lifecycle.fire(new AfterDestroy(id));
        } catch (Exception e) {
            this.state = State.DESTORY_FAILED;
            throw CubeControlException.failedDestroy(getId(), e);
        }
    }

    @Override
    public boolean isRunningOnRemote() {
        try {
            resource = client.update(resource);
            return isRunning(resource);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void changeToPreRunning() {
        // TODO Auto-generated method stub

    }

    @Override
    public Binding bindings() {
        if (holder != null) {
            return toBinding(holder.getPod());
        }
        return null;
    }

    @Override
    public Binding configuredBindings() {
        return toBinding(resource);
    }

    @Override
    public Void configuration() {
        return null;
    }

    private final class PortBindings implements HasPortBindings {

        private final Map<Integer, Integer> proxiedPorts;
        private final Map<Integer, PortAddress> mappedPorts;
        private final Set<Integer> containerPorts;
        private PortForwarder portForwarder;
        private Map<Integer, PortForwarder.PortForwardServer> portForwardServers =
            new HashMap<Integer, PortForwarder.PortForwardServer>();

        public PortBindings() {
            this.mappedPorts = new HashMap<Integer, PortAddress>();
            this.proxiedPorts = new LinkedHashMap<Integer, Integer>();
            for (String proxy : configuration.getProxiedContainerPorts()) {
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
                mappedPorts.put(containerPort,
                    new PortAddressImpl(getPortForwardBindAddress().getHostAddress(), mappedPort));
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
            return state == State.STARTED;
        }

        @Override
        public String getContainerIP() {
            if (isBound() && holder.getPod().getStatus() != null) {
                return holder.getPod().getStatus().getPodIP();
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

        @Override
        public InetAddress getPortForwardBindAddress() {
            try {
                return InetAddress.getByName(configuration.getPortForwardBindAddress());
            } catch (UnknownHostException e) {
                throw new IllegalArgumentException(e);
            }
        }

        private synchronized void podStarted() throws Exception {
            if (holder.getPod() != null && holder.getPod().getSpec() != null
                && holder.getPod().getSpec().getContainers() != null) {
                for (Container container : holder.getPod().getSpec().getContainers()) {
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
                portForwarder = new PortForwarder(client.getClient().getConfiguration(), getId());
            }
            try {
                portForwarder.setPortForwardBindAddress(getPortForwardBindAddress());
                for (Entry<Integer, Integer> mappedPort : proxiedPorts.entrySet()) {
                    PortForwarder.PortForwardServer server =
                        portForwarder.forwardPort(mappedPort.getValue(), mappedPort.getKey());
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

        private synchronized void podStopped() {
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
                try (ServerSocket serverSocket = new ServerSocket(port, 0, getPortForwardBindAddress())) {
                    return serverSocket.getLocalPort();
                }
            } catch (Throwable t) {
                throw new IllegalStateException("Could not allocate local port for forwarding proxy", t);
            }
        }
    }
}
