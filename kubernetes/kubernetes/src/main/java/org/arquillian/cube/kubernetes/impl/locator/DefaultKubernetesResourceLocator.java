package org.arquillian.cube.kubernetes.impl.locator;

import java.net.URL;
import java.util.Collection;
import java.util.Collections;

import org.arquillian.cube.kubernetes.api.KubernetesResourceLocator;

public class DefaultKubernetesResourceLocator implements KubernetesResourceLocator {

    private static final String ROOT = "/";
    private static final String[] RESOURCE_NAMES = new String[] {"kubernetes", "META-INF/fabric8/kubernetes"};
    private static final String[] ALLOWED_SUFFIXES = {".json", ".yml", ".yaml"};

    @Override
    public URL locate() {
        for (String resource : getResourceNames()) {
            for (String suffix : getAllowedSuffixes()) {
                URL candidate = getResource(resource + suffix);
                if (candidate != null) {
                    return candidate;
                }
            }
        }
        return null;
    }

    @Override
    public Collection<URL> locateAdditionalResources() {
        return Collections.emptyList();
    }

    protected String[] getResourceNames() {
        return RESOURCE_NAMES;
    }

    protected String[] getAllowedSuffixes() {
        return ALLOWED_SUFFIXES;
    }

    URL getResource(String resource) {
        return KubernetesResourceLocator.class.getResource(resource.startsWith(ROOT) ? resource : ROOT + resource);
    }

    @Override
    public KubernetesResourceLocator toImmutable() {
        return this;
    }
}
