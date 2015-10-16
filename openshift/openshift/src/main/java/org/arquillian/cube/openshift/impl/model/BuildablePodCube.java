package org.arquillian.cube.openshift.impl.model;

import static org.arquillian.cube.openshift.impl.client.ResourceUtil.isRunning;
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

import io.fabric8.kubernetes.api.model.Pod;

public class BuildablePodCube extends BaseCube {

    private String id;
    private Pod resource;
    private Template<Pod> template;
    private Cube.State state;
    private CubeOpenShiftConfiguration configuration;
    private OpenShiftClient client;

    private ResourceHolder holder;

    public BuildablePodCube(Pod resource, OpenShiftClient client, CubeOpenShiftConfiguration configuration) {
        this.id = resource.getMetadata().getName();
        this.resource = resource;
        this.template = new Template.PodTemplate(resource);
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
        try {
            holder = client.build(template);
            this.state = State.CREATED;
        } catch (Exception e) {
            this.state = State.CREATE_FAILED;
            throw CubeControlException.failedCreate(getId(), e);
        }
    }

    @Override
    public void start() throws CubeControlException {
        try {
            holder.setPod(client.createAndWait(holder.getPod()));
            this.state = State.STARTED;
        } catch (Exception e) {
            this.state = State.START_FAILED;
            throw CubeControlException.failedStart(getId(), e);
        }
    }

    @Override
    public void stop() throws CubeControlException {
        try {
            client.destroy(holder.getPod());
            this.state = State.STOPPED;
        } catch (Exception e) {
            this.state = State.STOP_FAILED;
            throw CubeControlException.failedStop(getId(), e);
        }
    }

    @Override
    public void destroy() throws CubeControlException {
        try {
            List<Exception> exceptions = client.clean(holder);
            if (exceptions.size() > 0) {
                throw exceptions.get(0);
            }
            this.state = State.DESTROYED;
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
    public Map<String, Object> configuration() {
        Map<String, Object> config = new HashMap<String, Object>();
        Map<String, Object> buildImage = new HashMap<String, Object>();
        if(template.getRefs().size() == 1) {
            buildImage.put("dockerfileLocation", template.getRefs().get(0).getPath());
        }
        config.put("buildImage", buildImage);
        return config;
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
