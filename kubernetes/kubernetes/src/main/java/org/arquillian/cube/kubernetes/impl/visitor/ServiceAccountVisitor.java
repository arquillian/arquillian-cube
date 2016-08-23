package org.arquillian.cube.kubernetes.impl.visitor;

import io.fabric8.kubernetes.api.builder.Visitor;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.arquillian.cube.impl.util.Strings;
import org.arquillian.cube.kubernetes.api.Configuration;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;

public class ServiceAccountVisitor implements Visitor {

    @Inject
    Instance<KubernetesClient> client;

    @Inject
    Instance<Configuration> configuration;

    @Override
    public void visit(Object element) {
        if (element instanceof PodSpecBuilder) {
            PodSpecBuilder builder = (PodSpecBuilder) element;
            String serviceAccount = builder.getServiceAccountName();
            if (Strings.isNotNullOrEmpty(serviceAccount)) {
                createServiceAccount(serviceAccount);
            }
        }
    }

    private void createServiceAccount(String serviceAccount) {
        KubernetesClient client = this.client.get();
        Configuration configuration = this.configuration.get();
        if (client.serviceAccounts().inNamespace(configuration.getNamespace()).withName(serviceAccount).get() == null) {
            client.serviceAccounts().inNamespace(configuration.getNamespace()).createNew()
                    .withNewMetadata()
                    .withName(serviceAccount)
                    .endMetadata()
                    .done();
        }
    }
 }
