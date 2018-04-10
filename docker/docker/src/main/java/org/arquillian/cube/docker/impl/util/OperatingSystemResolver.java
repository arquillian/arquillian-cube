package org.arquillian.cube.docker.impl.util;

public class OperatingSystemResolver {

    private String osNameProperty = System.getProperty("os.name");

    public OperatingSystemInterface currentOperatingSystem() {
        return OperatingSystem.resolve(osNameProperty);
    }
}
