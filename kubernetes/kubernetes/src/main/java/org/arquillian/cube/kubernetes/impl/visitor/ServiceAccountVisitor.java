package org.arquillian.cube.kubernetes.impl.visitor;

import io.fabric8.kubernetes.api.builder.v2_6.Visitor;
import io.fabric8.kubernetes.api.model.v2_6.PodSpecBuilder;
import io.fabric8.kubernetes.clnt.v2_6.KubernetesClient;
import org.arquillian.cube.impl.util.Strings;
import org.arquillian.cube.kubernetes.api.Configuration;
import org.arquillian.cube.kubernetes.api.Logger;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;

public class ServiceAccountVisitor implements Visitor {

    @Inject
    protected Instance<Logger> logger;
    @Inject
    Instance<KubernetesClient> client;
    @Inject
    Instance<Configuration> configuration;

    @Override
    public void visit(Object element) {
        if (element instanceof PodSpecBuilder) {
            PodSpecBuilder builder = (PodSpecBuilder) element;
            String serviceAccount = builder.getServiceAccountName();
            if (Strings.isNotNullOrEmpty(serviceAccount) && !serviceAccountExists(serviceAccount)) {
                try {
                    createServiceAccount(serviceAccount);
                } catch (Throwable t) {
                    logger.get().warn("Failed to create ServiceAccount with name:[" + serviceAccount + "].");
                }
            }
        }
    }

    private boolean serviceAccountExists(String serviceAccount) {
        KubernetesClient client = this.client.get();
        Configuration configuration = this.configuration.get();
        return client.serviceAccounts().inNamespace(configuration.getNamespace()).withName(serviceAccount).get() != null;
    }

    private void createServiceAccount(String serviceAccount) {
        KubernetesClient client = this.client.get();
        Configuration configuration = this.configuration.get();
        client.serviceAccounts().inNamespace(configuration.getNamespace()).createNew()
            .withNewMetadata()
            .withName(serviceAccount)
            .endMetadata()
            .done();
    }
}
