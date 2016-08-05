package org.arquillian.cube.kubernetes.impl.locator;

import org.arquillian.cube.kubernetes.api.KubernetesResourceLocator;

import java.net.URL;

/**
 * Created by iocanel on 8/1/16.
 */
public class DefaultKubernetesResouceLocator implements KubernetesResourceLocator {

    private static final String ROOT = "/";
    private static final String RESOURCE_NAME = "kubernetes";
    private static final String[] ALLOWED_SUFFIXES = {".json", ".yml", ".yaml"};


    @Override
    public URL locate() {
        for (String suffix : ALLOWED_SUFFIXES) {
            URL candidate = getResource(RESOURCE_NAME + suffix);
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }


    URL getResource(String resource) {
        return KubernetesResourceLocator.class.getResource(resource.startsWith(ROOT) ? resource : ROOT + resource);
    }

}
