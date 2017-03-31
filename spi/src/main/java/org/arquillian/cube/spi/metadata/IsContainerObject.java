package org.arquillian.cube.spi.metadata;


import org.arquillian.cube.containerobject.ConnectionMode;

public class IsContainerObject implements CubeMetadata {

    private Class<?> testClass;
    private ConnectionMode connectionMode = ConnectionMode.START_AND_STOP_AROUND_CLASS;

    public IsContainerObject(Class<?> testClass) {
        this.testClass = testClass;
    }

    public IsContainerObject(Class<?> testClass, ConnectionMode connectionMode) {
        this.testClass = testClass;
        this.connectionMode = connectionMode;
    }

    public Class<?> getTestClass() {
        return this.testClass;
    }

    public ConnectionMode getConnectionMode() {
        return connectionMode;
    }
}
