package org.arquillian.cube.kubernetes.api;

import java.net.URL;

/**
 * Created by iocanel on 8/1/16.
 */
public interface KubernetesResourceLocator {

    /**
     * Locates the kubernetes resource.
     * @return  Returns the url that points to the resource.
     */
    URL locate();
}
