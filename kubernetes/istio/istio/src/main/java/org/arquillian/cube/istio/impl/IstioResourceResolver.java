package org.arquillian.cube.istio.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

public class IstioResourceResolver {

    static final String URL_PREFIX = "http";
    static final String FILE_PREFIX = "file";
    static final String CLASSPATH_PREFIX = "classpath:";

    public static InputStream resolve(String location) {
        try {
            if (location.startsWith(URL_PREFIX) || location.startsWith(FILE_PREFIX)) {
                return new URL(location).openStream();
            } else if (location.startsWith(CLASSPATH_PREFIX)) {
                String classPathLocation = location.substring(location.indexOf(CLASSPATH_PREFIX)
                    + CLASSPATH_PREFIX.length());
                return Thread.currentThread().getContextClassLoader().getResource(classPathLocation).openStream();
            } else {
                return new ByteArrayInputStream(location.getBytes());
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

}
