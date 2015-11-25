package org.arquillian.cube.docker.impl.model;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.arquillian.cube.ChangeLog;
import org.arquillian.cube.TopContainer;
import org.arquillian.cube.docker.impl.await.AwaitStrategyFactory;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.docker.impl.util.BindingUtil;
import org.arquillian.cube.docker.impl.util.IOUtil;
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
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.annotation.Inject;

public class DockerCube extends BaseCube {

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
        return BindingUtil.binding(configuration);
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
    public Map<String, Object> configuration() {
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

    @Override
    public List<ChangeLog> changesOnFilesystem(String cubeId) {
        return executor.inspectChangesOnContainerFilesystem(cubeId);
    }

    @Override
    public void copyFileDirectoryFromContainer(String cubeId, String from,
            String to) {

        InputStream response = executor.getFileOrDirectoryFromContainerAsTar(cubeId, from);

        Path toPath = Paths.get(to);
        File toPathFile = toPath.toFile();

        if(toPathFile.exists() && toPathFile.isFile()) {
            throw new IllegalArgumentException(String.format("%s parameter should be a directory in copy operation but you set an already existing file not a directory. Check %s in your local directory because currently is a file.", "to", toPath.normalize().toString()));
        }

        try {
            Files.createDirectories(toPath);
            IOUtil.untar(response, toPathFile);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void copyLog(String containerId, boolean follow,
            boolean stdout, boolean stderr, boolean timestamps, int tail,
            OutputStream outputStream) {
        try {
            executor.copyLog(containerId, follow, stdout, stderr, timestamps, tail, outputStream);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public TopContainer top(String cubeId) {
        return executor.top(cubeId);
    }
}
