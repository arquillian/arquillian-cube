package org.arquillian.cube.spi;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import org.arquillian.cube.ChangeLog;
import org.arquillian.cube.TopContainer;

public interface Cube {

    public enum State {
        CREATED,
        CREATE_FAILED,
        STARTED,
        START_FAILED,
        STOPPED,
        STOP_FAILED,
        DESTROYED,
        DESTORY_FAILED,
        PRE_RUNNING
    }

    State state();

    String getId();

    void create() throws CubeControlException;

    void start() throws CubeControlException;

    void stop() throws CubeControlException;

    void destroy() throws CubeControlException;

    /**
    * Check the state of the Cube controller container on the remote server.
    * This should check the remote state of the Cube regardless of the current Cube.State.
    */
    boolean isRunningOnRemote();

    void changeToPreRunning();

    Binding bindings();

    Binding configuredBindings();

    Map<String, Object> configuration();

    boolean hasMetadata(Class<?> type);

    void addMetadata(Object type);

    <T> T getMetadata(Class<T> type);

    List<ChangeLog> changesOnFilesystem(String cubeId);

    void copyFileDirectoryFromContainer(String cubeId, String from, String to);

    void copyLog(String cubeId, boolean follow, boolean stdout, boolean stderr, boolean timestamps, int tail, OutputStream outputStream);
    
    TopContainer top(String cubeId);
}
