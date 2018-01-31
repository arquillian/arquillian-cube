package org.arquillian.cube.kubernetes.impl.utils;

import java.nio.file.Path;

public class ResourceFilter {

    public static boolean filterKubernetesResource(Path p) {
        return endsWith(p, ".yaml")
            || endsWith(p, ".yml")
            || endsWith(p, ".json");
    }

    private static boolean endsWith(Path path, String extension) {
        return path.toString().toLowerCase().endsWith(extension);
    }

}
