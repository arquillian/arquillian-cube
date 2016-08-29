package org.arquillian.cube.kubernetes.impl.namespace;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.arquillian.cube.kubernetes.api.Configuration;
import org.arquillian.cube.kubernetes.api.LabelProvider;
import org.arquillian.cube.kubernetes.api.Logger;
import org.arquillian.cube.kubernetes.api.NamespaceService;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;

import java.io.IOException;
import java.util.Map;

public class DefaultNamespaceService implements NamespaceService {

    private static final String PROJECT_LABEL = "project";
    private static final String FRAMEWORK_LABEL = "framework";
    private static final String COMPONENT_LABEL = "component";

    private static final String ARQUILLIAN_FRAMEWORK = "arquillian";
    private static final String ITEST_COMPONENT = "integrationTest";


    @Inject
    Instance<KubernetesClient> client;

    @Inject
    Instance<LabelProvider> labelProvider;

    @Inject
    Instance<Logger> logger;

    @Inject
    Instance<Configuration> configuration;

    @Override
    public Namespace create(String namespace) {
        return client.get().namespaces().createNew().withNewMetadata()
                .withName(namespace)
                .addToLabels(labelProvider.get().getLabels())
                .addToLabels(PROJECT_LABEL, client.get().getNamespace())
                .addToLabels(FRAMEWORK_LABEL, ARQUILLIAN_FRAMEWORK)
                .addToLabels(COMPONENT_LABEL, ITEST_COMPONENT)
                .endMetadata()
                .done();
    }

    @Override
    public Namespace annotate(String namespace, Map<String, String> annotations) {
        return client.get().namespaces().withName(namespace).edit()
                .editMetadata()
                .addToAnnotations(annotations)
                .endMetadata().done();
    }

    @Override
    public Boolean delete(String namespace) {
        return client.get().namespaces().withName(namespace).delete();
    }

    @Override
    public Boolean exists(String namespace) {
        return client.get().namespaces().withName(namespace).get() != null;
    }

    @Override
    public void clean(String namespace) {
        KubernetesClient client = this.client.get();
        client.extensions().deployments().inNamespace(namespace).delete();
        client.extensions().replicaSets().inNamespace(namespace).delete();
        client.replicationControllers().inNamespace(namespace).delete();
        client.pods().inNamespace(namespace).delete();
        client.extensions().ingresses().inNamespace(namespace).delete();
        client.services().inNamespace(namespace).delete();
        client.securityContextConstraints().withName(namespace).delete();
    }


    public void destroy(String namespace) {
        Logger logger = this.logger.get();
        Configuration configuration = this.configuration.get();
        KubernetesClient client = this.client.get();

        try {
            if (configuration.isNamespaceCleanupConfirmationEnabled()) {
                showErrors();
                logger.info("");
                logger.info("Waiting to destroy the namespace.");
                logger.info("Please type: [Q] to terminate the namespace.");

                while (true) {
                    try {
                        int ch = System.in.read();
                        if (ch < 0 || ch == 'Q') {
                            logger.info("Stopping...");
                            break;
                        } else {
                            logger.info("Found character: " + Character.toString((char) ch));
                        }
                    } catch (IOException e) {
                        logger.warn("Failed to read from input. " + e);
                        break;
                    }
                }
            } else {
                long timeout = configuration.getNamespaceCleanupTimeout();
                if (timeout > 0L) {
                    showErrors();
                    logger.info("");
                    logger.info("Sleeping for " + timeout + " seconds until destroying the namespace");
                    try {
                        Thread.sleep(timeout * 1000);
                    } catch (InterruptedException e) {
                        logger.info("Interupted sleeping to GC the namespace: " + e);
                    }
                }
            }
        } finally {
            client.namespaces().withName(namespace).delete();
        }
    }


    private void showErrors() {
    }

}
