package org.arquillian.cube.openshift.impl.model;

import static org.arquillian.cube.openshift.impl.client.ResourceUtil.toBinding;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.arquillian.cube.ChangeLog;
import org.arquillian.cube.TopContainer;
import org.arquillian.cube.openshift.impl.client.CubeOpenShiftConfiguration;
import org.arquillian.cube.openshift.impl.client.OpenShiftClient;
import org.arquillian.cube.openshift.impl.client.OpenShiftClient.ResourceHolder;
import org.arquillian.cube.spi.BaseCube;
import org.arquillian.cube.spi.Binding;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeControlException;

import io.fabric8.kubernetes.api.model.Service;

public class ServiceCube extends BaseCube {

    private String id;
    private Service resource;
    private State state;
    private CubeOpenShiftConfiguration configuration;
    private OpenShiftClient client;

    private ResourceHolder holder;

    public ServiceCube(Service resource, OpenShiftClient client, CubeOpenShiftConfiguration configuration) {
        this.id = resource.getMetadata().getName();
        this.resource = resource;
        this.client = client;
        this.configuration = configuration;
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
        this.state = State.CREATED;
    }

    @Override
    public void start() throws CubeControlException {
        try {
            resource = client.create(resource);
            this.state = State.STARTED;
        } catch (Exception e) {
            this.state = State.START_FAILED;
            throw CubeControlException.failedStart(getId(), e);
        }
    }

    @Override
    public void stop() throws CubeControlException {
        try {
            client.destroy(resource);
            this.state = State.STOPPED;
        } catch (Exception e) {
            this.state = State.STOP_FAILED;
            throw CubeControlException.failedStop(getId(), e);
        }
    }

    @Override
    public void destroy() throws CubeControlException {
        this.state = State.DESTROYED;
    }

    @Override
    public boolean isRunningOnRemote() {
        return state == State.STARTED;
    }

    @Override
    public void changeToPreRunning() {
        // TODO Auto-generated method stub

    }

    @Override
    public Binding bindings() {
        if (holder != null) {
            return toBinding(resource);
        }
        return null;
    }

    @Override
    public Binding configuredBindings() {
        return toBinding(resource);
    }

    @Override
    public Map<String, Object> configuration() {
        return new HashMap<String, Object>();
    }

    @Override
    public List<ChangeLog> changesOnFilesystem(String cubeId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void copyFileDirectoryFromContainer(String cubeId, String from, String to) {
        // TODO Auto-generated method stub

    }

    @Override
    public void copyLog(String cubeId, boolean follow, boolean stdout, boolean stderr, boolean timestamps, int tail,
            OutputStream outputStream) {
        // TODO Auto-generated method stub

    }

    @Override
    public TopContainer top(String cubeId) {
        // TODO Auto-generated method stub
        return null;
    }
}
