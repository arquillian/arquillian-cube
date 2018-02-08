package org.arquillian.cube.kubernetes.impl.resolver;

import java.io.IOException;
import java.net.URL;

public class ResourceResolver {

    static final String URL_PREFIX = "http";
    static final String FILE_PREFIX = "file";
    static final String CLASSPATH_PREFIX = "classpath:";

    public static URL resolve(String location) {
        try {
            if (location.startsWith(URL_PREFIX) || location.startsWith(FILE_PREFIX)) {
                return new URL(location);
            } else if (location.startsWith(CLASSPATH_PREFIX)) {
                String classPathLocation = location.substring(location.indexOf(CLASSPATH_PREFIX)
                    + CLASSPATH_PREFIX.length());
                final URL resource = Thread.currentThread().getContextClassLoader().getResource(classPathLocation);

                if (resource == null) {
                    throw new IllegalArgumentException(String.format("%s location couldn't be found inside classpath.", classPathLocation));
                }

                return resource;
            } else {
                return new URL(location);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
