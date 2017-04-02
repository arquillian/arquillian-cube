package org.arquillian.cube.openshift.impl.namespace;

import io.fabric8.kubernetes.api.model.v2_2.Namespace;
import io.fabric8.kubernetes.clnt.v2_2.KubernetesClient;
import io.fabric8.kubernetes.clnt.v2_2.KubernetesClientException;
import io.fabric8.openshift.api.model.v2_2.ProjectRequest;
import io.fabric8.openshift.api.model.v2_2.ProjectRequestBuilder;
import io.fabric8.openshift.clnt.v2_2.OpenShiftClient;
import java.util.Map;
import org.arquillian.cube.kubernetes.api.Configuration;
import org.arquillian.cube.kubernetes.api.LabelProvider;
import org.arquillian.cube.kubernetes.api.Logger;
import org.arquillian.cube.kubernetes.api.NamespaceService;
import org.arquillian.cube.kubernetes.impl.namespace.DefaultNamespaceService;

public class OpenshiftNamespaceService extends DefaultNamespaceService {

    @Override
    public NamespaceService toImmutable() {
        if (delegate != null) {
            return delegate;
        }
        synchronized (this) {
            if (delegate == null) {
                delegate = new ImmutableOpenshiftNamespaceService(client.get(),
                    configuration.get(),
                    labelProvider.get().toImmutable(),
                    logger.get().toImmutable()
                );
            }
        }
        return delegate;
    }

    public static class ImmutableOpenshiftNamespaceService extends DefaultNamespaceService.ImmutableNamespaceService {

        public ImmutableOpenshiftNamespaceService(KubernetesClient client, Configuration configuration,
            LabelProvider labelProvider, Logger logger) {
            super(client, configuration, labelProvider, logger);
        }

        @Override
        public Namespace create(String namespace, Map<String, String> annotations) {
            OpenShiftClient openShiftClient = client.adapt(OpenShiftClient.class);
            ProjectRequest projectRequest = new ProjectRequestBuilder()
                .withNewMetadata()
                .withName(namespace)
                .withAnnotations(annotations)
                .addToLabels(labelProvider.getLabels())
                .addToLabels(PROJECT_LABEL, client.getNamespace())
                .addToLabels(FRAMEWORK_LABEL, ARQUILLIAN_FRAMEWORK)
                .addToLabels(COMPONENT_LABEL, ITEST_COMPONENT)
                .endMetadata()
                .build();

            ProjectRequest request = openShiftClient.projectrequests().create(projectRequest);
            return openShiftClient.namespaces().withName(request.getMetadata().getName()).get();
        }

        @Override
        public Namespace create(String namespace) {
            if (client.isAdaptable(OpenShiftClient.class)) {
                OpenShiftClient openShiftClient = client.adapt(OpenShiftClient.class);
                ProjectRequest projectRequest = new ProjectRequestBuilder()
                    .withNewMetadata()
                    .withName(namespace)
                    .addToLabels(labelProvider.getLabels())
                    .addToLabels(PROJECT_LABEL, client.getNamespace())
                    .addToLabels(FRAMEWORK_LABEL, ARQUILLIAN_FRAMEWORK)
                    .addToLabels(COMPONENT_LABEL, ITEST_COMPONENT)
                    .endMetadata()
                    .build();

                ProjectRequest request = openShiftClient.projectrequests().create(projectRequest);
                return openShiftClient.namespaces().withName(request.getMetadata().getName()).get();
            } else {
                return super.create(namespace);
            }
        }

        @Override
        public Boolean delete(String namespace) {
            if (client.isAdaptable(OpenShiftClient.class)) {
                OpenShiftClient openShiftClient = client.adapt(OpenShiftClient.class);
                return openShiftClient.projects().withName(namespace).delete();
            } else {
                return super.delete(namespace);
            }
        }

        @Override
        public Boolean exists(String namespace) {
            if (client.isAdaptable(OpenShiftClient.class)) {
                OpenShiftClient openShiftClient = client.adapt(OpenShiftClient.class);
                try {
                    return openShiftClient.projects().withName(namespace).get() != null;
                } catch (KubernetesClientException e) {
                    return false;
                }
            } else {
                return super.exists(namespace);
            }
        }

        @Override
        public Namespace annotate(String namespace, Map<String, String> annotations) {
            if (client.isAdaptable(OpenShiftClient.class)) {
                OpenShiftClient openShiftClient = client.adapt(OpenShiftClient.class);
                openShiftClient.projects().withName(namespace)
                    .edit()
                    .editMetadata()
                    .addToAnnotations(annotations)
                    .endMetadata()
                    .done();

                return openShiftClient.namespaces().withName(namespace).get();
            } else {
                return super.annotate(namespace, annotations);
            }
        }

        @Override
        @Deprecated // The method is redundant (since its called always before destroy).
        public void clean(String namespace) {
            if (client.isAdaptable(OpenShiftClient.class)) {
                OpenShiftClient openShiftClient = client.adapt(OpenShiftClient.class);
                openShiftClient.deploymentConfigs().inNamespace(namespace).delete();
            }

            super.clean(namespace);
        }
    }
}
