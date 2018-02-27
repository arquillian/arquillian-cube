package org.arquillian.cube.docker.impl.util;

public interface OperatingSystemInterface {
    String getLabel();

    OperatingSystemFamilyInterface getFamily();

    OperatingSystemFamilyInterface getDefaultFamily();
}
