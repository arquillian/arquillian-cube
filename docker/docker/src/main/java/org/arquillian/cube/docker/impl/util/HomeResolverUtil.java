package org.arquillian.cube.docker.impl.util;

public class HomeResolverUtil {

    public static String resolveHomeDirectoryChar(String path) {
        if(path.startsWith("~")) {
            return path.replace("~", System.getProperty("user.home"));
        }
        return path;
    }
}
