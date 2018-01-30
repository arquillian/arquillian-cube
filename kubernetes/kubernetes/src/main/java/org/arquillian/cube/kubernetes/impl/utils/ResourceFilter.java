package org.arquillian.cube.kubernetes.impl.utils;

import java.nio.file.Path;

public class ResourceFilter {

    public static boolean filterKubernetesResource(Path p) {
        return p.toString().toLowerCase().endsWith(".yaml")
            || p.toString().toLowerCase().endsWith(".yml")
            || p.toString().toLowerCase().endsWith(".json");
    }
}
