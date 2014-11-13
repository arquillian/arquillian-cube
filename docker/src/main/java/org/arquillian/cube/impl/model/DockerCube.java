package org.arquillian.cube.impl.model;

import java.util.Map;
import java.util.logging.Logger;

import org.arquillian.cube.impl.await.AwaitStrategyFactory;
import org.arquillian.cube.impl.docker.DockerClientExecutor;
import org.arquillian.cube.impl.util.BindingUtil;
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
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.annotation.Inject;

public class DockerCube implements Cube {

    private static final Logger log = Logger.getLogger(DockerCube.class.getName());

    private State state = State.DESTROYED;
    private String id;
    private Binding binding = null;

    private Map<String, Object> configuration;

    @Inject
    private Event<CubeLifecyleEvent> lifecycle;

    private DockerClientExecutor executor;

    public DockerCube(String id, Map<String, Object> configuration, DockerClientExecutor executor) {
        this.id = id;
        this.configuration = configuration;
        this.executor = executor;
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
        if(state == State.STARTED) {
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
        if(state == State.STOPPED) {
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
        if(state != State.STARTED) {
            throw new IllegalStateException("Can't get binding for cube " + id + " when status not " + State.STARTED + ". Status is " + state);
        }
        binding = BindingUtil.binding(executor, id);
        return binding;
    }

    @Override
    public Map<String, Object> configuration() {
        return configuration;
    }
}
