package org.arquillian.cube.kubernetes.impl.namespace;

import io.fabric8.kubernetes.api.model.v2_6.Namespace;
import io.fabric8.kubernetes.clnt.v2_6.KubernetesClient;
import java.util.Collections;
import java.util.Map;
import java.util.Scanner;
import org.arquillian.cube.kubernetes.api.Configuration;
import org.arquillian.cube.kubernetes.api.LabelProvider;
import org.arquillian.cube.kubernetes.api.Logger;
import org.arquillian.cube.kubernetes.api.NamespaceService;
import org.arquillian.cube.kubernetes.api.WithToImmutable;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.spi.Validate;

public class DefaultNamespaceService implements NamespaceService {

    protected static final String PROJECT_LABEL = "project";
    protected static final String FRAMEWORK_LABEL = "framework";
    protected static final String COMPONENT_LABEL = "component";

    protected static final String ARQUILLIAN_FRAMEWORK = "arquillian";
    protected static final String ITEST_COMPONENT = "integrationTest";

    @Inject
    protected Instance<KubernetesClient> client;

    @Inject
    protected Instance<LabelProvider> labelProvider;

    @Inject
    protected Instance<Logger> logger;

    @Inject
    protected Instance<Configuration> configuration;

    protected NamespaceService delegate;

    @Override
    public NamespaceService toImmutable() {
        if (delegate != null) {
            return delegate;
        }
        synchronized (this) {
            if (delegate == null) {
                delegate = new ImmutableNamespaceService(client.get(), configuration.get(),
                    labelProvider.get().toImmutable(),
                    logger.get().toImmutable());
            }
        }
        return delegate;
    }

    @Override
    public Namespace create(String namespace) {
        return toImmutable().create(namespace);
    }

    @Override
    public Namespace create(String namespace, Map<String, String> annotations) {
        return toImmutable().create(namespace, annotations);
    }

    @Override
    public Namespace annotate(String namespace, Map<String, String> annotations) {
        return toImmutable().annotate(namespace, annotations);
    }

    @Override
    public Boolean delete(String namespace) {
        return toImmutable().delete(namespace);
    }

    @Override
    public Boolean exists(String namespace) {
        return toImmutable().exists(namespace);
    }

    @Override
    @Deprecated // The method is redundant (since its called always before destroy).
    public void clean(String namespace) {
        toImmutable().clean(namespace);
    }

    public void destroy(String namespace) {
        toImmutable().destroy(namespace);
    }

    public static class ImmutableNamespaceService implements NamespaceService, WithToImmutable<NamespaceService> {

        protected final KubernetesClient client;
        protected final LabelProvider labelProvider;
        protected final Logger logger;
        protected final Configuration configuration;

        public ImmutableNamespaceService(KubernetesClient client, Configuration configuration,
            LabelProvider labelProvider, Logger logger) {
            Validate.notNull(client, "A KubernetesClient instance is required.");
            Validate.notNull(labelProvider, "A LabelProvider  instance is required.");
            Validate.notNull(logger, "A Logger instance is required.");
            Validate.notNull(configuration, "Configuration is required.");
            this.client = client;
            this.configuration = configuration;
            this.labelProvider = labelProvider;
            this.logger = logger;
        }

        @Override
        public Namespace create(String namespace) {
            return create(namespace, Collections.emptyMap());
        }

        @Override
        public Namespace create(String namespace, Map<String, String> annotations) {
            logger.status("Creating namespace: " + namespace + "...");
            Namespace result = client.namespaces().createNew().withNewMetadata()
                .withName(namespace)
                .withAnnotations(annotations)
                .addToLabels(labelProvider.getLabels())
                .addToLabels(PROJECT_LABEL, client.getNamespace())
                .addToLabels(FRAMEWORK_LABEL, ARQUILLIAN_FRAMEWORK)
                .addToLabels(COMPONENT_LABEL, ITEST_COMPONENT)
                .endMetadata()
                .done();
            logger.info(
                "To switch to the new namespace: kubectl config set-context `kubectl config current-context` --namespace="
                    + namespace);
            return result;
        }

        @Override
        public Namespace annotate(String namespace, Map<String, String> annotations) {
            return client.namespaces().withName(namespace).edit()
                .editMetadata()
                .addToAnnotations(annotations)
                .endMetadata().done();
        }

        @Override
        public Boolean delete(String namespace) {
            logger.info("Deleting namespace: " + namespace + "...");
            Boolean deleted = client.namespaces().withName(namespace).delete();
            if (deleted) {
                logger.info("Namespace: " + namespace + ", successfully deleted");
            }
            return deleted;
        }

        @Override
        public Boolean exists(String namespace) {
            return client.namespaces().withName(namespace).get() != null;
        }

        @Override
        @Deprecated // The method is redundant (since its called always before destroy).
        public void clean(String namespace) {
            KubernetesClient client = this.client;
            client.extensions().deployments().inNamespace(namespace).delete();
            client.extensions().replicaSets().inNamespace(namespace).delete();
            client.replicationControllers().inNamespace(namespace).delete();
            client.pods().inNamespace(namespace).delete();
            client.extensions().ingresses().inNamespace(namespace).delete();
            client.services().inNamespace(namespace).delete();
            client.securityContextConstraints().withName(namespace).delete();
        }

        public void destroy(String namespace) {
            Logger logger = this.logger;
            Configuration configuration = this.configuration;
            try {
                if (configuration.isNamespaceDestroyConfirmationEnabled()) {
                    showErrors();
                    logger.info("");
                    logger.info("Waiting to destroy the namespace.");
                    logger.info("Please press <enter> to cleanup the namespace.");

                    Scanner scanner = new Scanner(System.in);
                    scanner.nextLine();
                    logger.info("Cleaning up...");
                    return;
                } else {
                    long timeout = configuration.getNamespaceDestroyTimeout();
                    if (timeout > 0L) {
                        showErrors();
                        logger.info("");
                        logger.info("Waiting for " + timeout + " seconds before destroying the namespace");
                        try {
                            Thread.sleep(timeout * 1000);
                        } catch (InterruptedException e) {
                            logger.info("Interrupted waiting to GC the namespace: " + e);
                        }
                    }
                }
            } finally {
                delete(namespace);
            }
        }

        private void showErrors() {
        }

        @Override
        public NamespaceService toImmutable() {
            return this;
        }
    }
}
