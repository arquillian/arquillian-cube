package org.arquillian.cube.kubernetes.impl;

import java.net.URL;
import java.util.Optional;

public class KubernetesAssistantDefaultResourceLocator {

    private static final String[] RESOURCE_NAMES =
        new String[] {"openshift", "META-INF/fabric8/openshift", "kubernetes", "META-INF/fabric8/kubernetes"};

    private static final String[] ALLOWED_SUFFIXES = {".json", ".yml", ".yaml"};

    public Optional<URL> locate() {
        for (String resource : getResourceNames()) {
            for (String suffix : getAllowedSuffixes()) {
                URL candidate = getResource(resource + suffix);
                if (candidate != null) {
                    return Optional.of(candidate);
                }
            }
        }
        return Optional.empty();
    }

    private String[] getResourceNames() {
        return RESOURCE_NAMES;
    }

    private String[] getAllowedSuffixes() {
        return ALLOWED_SUFFIXES;
    }

    private URL getResource(String resource) {
        return Thread.currentThread().getContextClassLoader().getResource(resource);
    }

}
