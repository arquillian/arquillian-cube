package org.arquillian.cube.kubernetes.api;

import java.net.URL;
import java.util.Collection;

public interface KubernetesResourceLocator extends WithToImmutable<KubernetesResourceLocator> {

    /**
     * Locates the main kubernetes resource.
     *
     * @return Returns the url that points to the resource.
     */
    URL locate();

    /**
     * Locate additional resources (such as ImageStreams) that
     * should be created in the test namespace.
     *
     * @return a collection of urls to additional resources.
     */
    Collection<URL> locateAdditionalResources();
}
