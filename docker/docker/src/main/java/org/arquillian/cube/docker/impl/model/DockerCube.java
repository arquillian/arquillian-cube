package org.arquillian.cube.docker.impl.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.arquillian.cube.docker.impl.await.AwaitStrategyFactory;
import org.arquillian.cube.docker.impl.client.config.CubeContainer;
import org.arquillian.cube.docker.impl.client.metadata.ChangesOnFilesystem;
import org.arquillian.cube.docker.impl.client.metadata.CopyFromContainer;
import org.arquillian.cube.docker.impl.client.metadata.GetTop;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.docker.impl.util.BindingUtil;
import org.arquillian.cube.spi.BaseCube;
import org.arquillian.cube.spi.Binding;
import org.arquillian.cube.spi.Binding.PortBinding;
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
import org.arquillian.cube.spi.metadata.CanSeeChangesOnFilesystem;
import org.arquillian.cube.spi.metadata.CanSeeTop;
import org.arquillian.cube.spi.metadata.HasPortBindings;
import org.arquillian.cube.spi.metadata.IsBuildable;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.annotation.Inject;

import com.github.dockerjava.api.exception.NotFoundException;

public class DockerCube extends BaseCube<CubeContainer> {

    private static final Logger log = Logger.getLogger(DockerCube.class.getName());

    private State state = State.DESTROYED;
    private String id;
    private Binding binding = null;

    private CubeContainer configuration;

    private final PortBindings portBindings;

    @Inject
    private Event<CubeLifecyleEvent> lifecycle;

    private DockerClientExecutor executor;

    public DockerCube(String id, CubeContainer configuration, DockerClientExecutor executor) {
        this.id = id;
        this.configuration = configuration;
        this.executor = executor;
        this.portBindings = new PortBindings();
        addDefaultMetadata();
    }

    private void addDefaultMetadata() {
        addMetadata(CanCopyFromContainer.class, new CopyFromContainer(getId(), executor));
        addMetadata(CanSeeChangesOnFilesystem.class, new ChangesOnFilesystem(getId(), executor));
        addMetadata(CanSeeTop.class, new GetTop(getId(), executor));
        addMetadata(HasPortBindings.class, portBindings);
        if(configuration.getBuildImage() !=null) {
            String path = configuration.getBuildImage().getDockerfileLocation();
            if(path != null) {
                addMetadata(IsBuildable.class, new IsBuildable(path));
            }
        }
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
        if(state != State.DESTROYED) {
            return;
        }
        try {
            lifecycle.fire(new BeforeCreate(id));
            log.fine(String.format("Creating container with name %s and configuration %s.", id, configuration));
            executor.createContainer(id, configuration);
            log.fine(String.format("Created container with id %s.", id));
            state = State.CREATED;
            lifecycle.fire(new AfterCreate(id));
        } catch(Exception e) {
            state = State.CREATE_FAILED;
            throw CubeControlException.failedCreate(id, e);
        }
    }

    @Override
    public void start() throws CubeControlException {
        if(state == State.STARTED || state == State.PRE_RUNNING) {
            return;
        }
        try {
            lifecycle.fire(new BeforeStart(id));
            executor.startContainer(id, configuration);
            state = State.STARTED;
            portBindings.containerStarted();
            if(!AwaitStrategyFactory.create(executor, this, configuration).await()) {
                throw new IllegalArgumentException(String.format("Cannot connect to %s container", id));
            }
            lifecycle.fire(new AfterStart(id));
        } catch(Exception e) {
            state = State.START_FAILED;
            throw CubeControlException.failedStart(id, e);
        }
    }

    @Override
    public void stop() throws CubeControlException {
        if(state == State.STOPPED || state == State.PRE_RUNNING) {
            return;
        }
        try {
            lifecycle.fire(new BeforeStop(id));
            try {
                executor.stopContainer(id);
            } catch(NotFoundException e) {}
            state = State.STOPPED;
            lifecycle.fire(new AfterStop(id));
        } catch(Exception e) {
            state = State.STOP_FAILED;
            throw CubeControlException.failedStop(id, e);
        }
    }

    @Override
    public void destroy() throws CubeControlException {
        if(state != State.STOPPED) {
            return;
        }
        try {
            lifecycle.fire(new BeforeDestroy(id));
            try {
                executor.removeContainer(id, configuration.getRemoveVolumes());
            } catch(NotFoundException e) {}
            state = State.DESTROYED;
            lifecycle.fire(new AfterDestroy(id));
        } catch(Exception e) {
            state = State.DESTORY_FAILED;
            throw CubeControlException.failedDestroy(id, e);
        }
    }

    @Override
    public Binding bindings() {
        if(binding != null) {
            return binding;
        }
        if(state != State.STARTED && state != State.PRE_RUNNING) {
            throw new IllegalStateException("Can't get binding for cube " + id + " when status not " + State.STARTED + " or " + State.PRE_RUNNING + ". Status is " + state);
        }
        binding = BindingUtil.binding(executor, id);
        return binding;
    }

    @Override
    public Binding configuredBindings() {
        return BindingUtil.binding(configuration, executor);
    }

    @Override
    public boolean isRunningOnRemote() {
       // TODO should we create an adapter class so we don't expose client classes in this part?
       List<com.github.dockerjava.api.model.Container> runningContainers = executor.listRunningContainers();
       for (com.github.dockerjava.api.model.Container container : runningContainers) {
           for (String name : container.getNames()) {
               if (name.startsWith("/"))
                   name = name.substring(1); // Names array adds an slash to the docker name container.
               if (name.equals(getId())) { // cube id is the container name in docker0 Id in docker is the hash
                                                // that identifies it.
                   return true;
               }
           }
       }

       return false;
    }

    @Override
    public CubeContainer configuration() {
        return configuration;
    }

    @Override
    public void changeToPreRunning() {
        if(state != State.DESTROYED && state != State.STARTED) {
            return;
        }

        log.fine(String.format("Reusing prerunning container with name %s and configuration %s.", id, configuration));
        state = State.PRE_RUNNING;
    }
    
    private class PortBindings implements HasPortBindings {

        private final Map<Integer, PortAddress> mappedPorts;
        private final Set<Integer> containerPorts;
        private final Set<Integer> boundPorts;
        private String containerIP;
        private String internalIP;

        private PortBindings() {
            this.mappedPorts = new HashMap<Integer, PortAddress>();
            this.containerPorts = new LinkedHashSet<Integer>();
            final Binding configuredBindings = configuredBindings();
            containerIP = configuredBindings.getIP();
            for (PortBinding portBinding : configuredBindings.getPortBindings()) {
                final int exposedPort = portBinding.getExposedPort();
                final Integer boundPort = portBinding.getBindingPort();
                containerPorts.add(exposedPort);
                if (boundPort != null && containerIP != null) {
                    mappedPorts.put(exposedPort, new PortAddressImpl(containerIP, boundPort));
                }
            }
            this.boundPorts = new LinkedHashSet<Integer>(containerPorts.size());
        }

        @Override
        public boolean isBound() {
            return EnumSet.of(State.PRE_RUNNING, State.STARTED).contains(state);
        }

        @Override
        public synchronized String getContainerIP() {
            return containerIP;
        }

        @Override
        public String getInternalIP() {
            return internalIP;
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
        
        /*
         * Initialize bound ports and regenerate port mappings
         */
        private synchronized void containerStarted() {
            final Binding bindings = bindings();
            containerIP = bindings.getIP();
            internalIP = bindings.getInternalIP();
            for (PortBinding portBinding : bindings.getPortBindings()) {
                final int exposedPort = portBinding.getExposedPort();
                final Integer boundPort = portBinding.getBindingPort();
                boundPorts.add(exposedPort);
                if (boundPort != null && containerIP != null) {
                    // just overwrite existing entries
                    mappedPorts.put(exposedPort, new PortAddressImpl(containerIP, boundPort));
                }
            }
        }
    }
}
