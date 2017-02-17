package org.arquillian.cube.kubernetes.impl.locator;

import org.arquillian.cube.kubernetes.api.KubernetesResourceLocator;

import java.net.URL;

public class DefaultKubernetesResouceLocator implements KubernetesResourceLocator {

    private static final String ROOT = "/";
    private static final String[] RESOURCE_NAMES = new String[] { "kubernetes", "META-INF/fabric8/kubernetes" };
    private static final String[] ALLOWED_SUFFIXES = {".json", ".yml", ".yaml"};

    @Override
    public URL locate() {
        for (String suffix : ALLOWED_SUFFIXES) {
            for (String resource : RESOURCE_NAMES) {
                URL candidate = getResource(resource + suffix);
                if (candidate != null) {
                    return candidate;
                }
            }
        }
        return null;
    }

    URL getResource(String resource) {
        return KubernetesResourceLocator.class.getResource(resource.startsWith(ROOT) ? resource : ROOT + resource);
    }

    @Override
    public KubernetesResourceLocator toImmutable() {
        return this;
    }
}
