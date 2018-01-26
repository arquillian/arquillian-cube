package org.arquillian.cube.istio.impl;

import java.net.MalformedURLException;
import java.net.URL;

public class IstioResourceResolver {

    static final String URL_PREFIX = "http";
    static final String FILE_PREFIX = "file";

    public static URL resolve(String location) {
        try {
            if (location.startsWith(URL_PREFIX) || location.startsWith(FILE_PREFIX)) {
                return new URL(location);
            } else {
                return Thread.currentThread().getContextClassLoader().getResource(location);
            }
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

}
