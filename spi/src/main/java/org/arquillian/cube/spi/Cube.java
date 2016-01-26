package org.arquillian.cube.spi;

import org.arquillian.cube.spi.metadata.CubeMetadata;

public interface Cube<T> {

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

    /**
     * @return actual binding of running container
     */
    Binding bindings();

    /**
     * @return binding as configured by meta-data, e.g. EXPOSE or pod.json
     */
    Binding configuredBindings();

    T configuration();

    <X extends CubeMetadata> boolean hasMetadata(Class<X> type);

    <X extends CubeMetadata> void addMetadata(Class<X> type, X impl);

    <X extends CubeMetadata> X getMetadata(Class<X> type);
}
