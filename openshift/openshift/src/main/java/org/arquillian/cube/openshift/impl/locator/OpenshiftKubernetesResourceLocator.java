package org.arquillian.cube.openshift.impl.locator;

import io.fabric8.kubernetes.clnt.v2_6.KubernetesClient;
import io.fabric8.openshift.clnt.v2_6.OpenShiftClient;
import org.arquillian.cube.kubernetes.impl.locator.DefaultKubernetesResourceLocator;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;

public class OpenshiftKubernetesResourceLocator extends DefaultKubernetesResourceLocator {

    private static final String[] RESOURCE_NAMES =
        new String[] {"openshift", "META-INF/fabric8/openshift", "kubernetes", "META-INF/fabric8/kubernetes"};

    @Inject
    protected Instance<KubernetesClient> client;

    @Override
    protected String[] getResourceNames() {
        if (!client.get().isAdaptable(OpenShiftClient.class)) {
            return super.getResourceNames();
        }
        return RESOURCE_NAMES;
    }

}
