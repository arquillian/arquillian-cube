package org.arquillian.cube.openshift.impl.enricher;

import org.arquillian.cube.kubernetes.impl.enricher.AbstractKubernetesResourceProvider;

import io.fabric8.openshift.client.OpenShiftClient;

public abstract class AbstractOpenshiftResourceProvider extends AbstractKubernetesResourceProvider {

    protected OpenShiftClient getOpenshiftClient() {
        return getClient().adapt(OpenShiftClient.class);
    }

}
