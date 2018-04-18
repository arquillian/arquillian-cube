package org.arquillian.cube.impl.model;

import java.util.Map;

public class ContainerMetadata {

    private final Map<String, String> names;

    public ContainerMetadata(Map<String, String> names) {
        this.names = names;
    }

    public Map<String, String> getNames() {
        return names;
    }
}
