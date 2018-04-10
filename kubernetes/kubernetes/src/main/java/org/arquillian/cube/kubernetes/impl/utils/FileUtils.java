package org.arquillian.cube.kubernetes.impl.utils;

import java.nio.file.Path;
import java.util.Arrays;

public class FileUtils {

    public static boolean isResourceAllowed(Path path, String[] resourceNames, String[] resourceSuffixes) {
        return Arrays.asList(resourceNames).contains(resourceName(path))
            && Arrays.asList(resourceSuffixes).contains(resourceSuffix(path));
    }


    static String resourceSuffix(Path path) {
        final String name = fileName(path);

        return name.substring(name.lastIndexOf("."));
    }

    static String resourceName(Path path) {
        final String name = fileName(path);

        return name.substring(0, name.lastIndexOf("."));
    }

    private static String fileName(Path path) {
        return path.getFileName().toString();
    }
}
