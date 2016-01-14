package org.arquillian.cube.docker.impl.model;

import java.util.List;
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
import org.arquillian.cube.spi.metadata.IsBuildable;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.annotation.Inject;

public class DockerCube extends BaseCube<CubeContainer> {

    private static final Logger log = Logger.getLogger(DockerCube.class.getName());

    private State state = State.DESTROYED;
    private String id;
    private Binding binding = null;

    private CubeContainer configuration;

    @Inject
    private Event<CubeLifecyleEvent> lifecycle;

    private DockerClientExecutor executor;

    public DockerCube(String id, CubeContainer configuration, DockerClientExecutor executor) {
        this.id = id;
        this.configuration = configuration;
        this.executor = executor;
        addDefaultMetadata();
    }

    private void addDefaultMetadata() {
        addMetadata(CanCopyFromContainer.class, new CopyFromContainer(getId(), executor));
        addMetadata(CanSeeChangesOnFilesystem.class, new ChangesOnFilesystem(getId(), executor));
        addMetadata(CanSeeTop.class, new GetTop(getId(), executor));
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
            executor.stopContainer(id);
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
            executor.removeContainer(id);
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
}
