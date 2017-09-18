package org.arquillian.cube.kubernetes.api;

import io.fabric8.kubernetes.api.model.v2_6.HasMetadata;
import java.net.URL;
import java.util.List;
import java.util.Map;

public interface ResourceInstaller extends WithToImmutable<ResourceInstaller> {

    /**
     * Installs the resources found in the specified URL.
     *
     * @param url
     *     The URL to read resources from.
     *
     * @return The list with the created resources.
     */
    List<HasMetadata> install(URL url);

    /**
     * Uninstalls the resources found in the specified URL.
     *
     * @param url
     *     The URL to read resources from.
     *
     * @return A map of the resources to their delete status.
     */
    Map<HasMetadata, Boolean> uninstall(URL url);

    /**
     * Uninstalls the resources found in the specified list.
     *
     * @param list
     *     The list with the resources.
     *
     * @return A map of the resources to their delete status.
     */
    Map<HasMetadata, Boolean> uninstall(List<HasMetadata> list);
}
