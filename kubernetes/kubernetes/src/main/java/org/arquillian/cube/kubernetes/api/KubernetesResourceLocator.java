package org.arquillian.cube.kubernetes.api;

import java.net.URL;

public interface KubernetesResourceLocator {

    /**
     * Locates the kubernetes resource.
     * @return  Returns the url that points to the resource.
     */
    URL locate();
}
