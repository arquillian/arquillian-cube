package org.arquillian.cube.openshift.impl.namespace;

import org.arquillian.cube.kubernetes.impl.namespace.DefaultNamespaceService;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.openshift.api.model.ProjectRequest;
import io.fabric8.openshift.api.model.ProjectRequestBuilder;
import io.fabric8.openshift.client.OpenShiftClient;

public class OpenshiftNamespaceService extends DefaultNamespaceService {

    private static final String PROJECT_LABEL = "project";
    private static final String FRAMEWORK_LABEL = "framework";
    private static final String COMPONENT_LABEL = "component";

    private static final String ARQUILLIAN_FRAMEWORK = "arquillian";
    private static final String ITEST_COMPONENT = "integrationTest";

    @Override
    public Namespace create(String namespace) {
        KubernetesClient client = this.client.get();
        if (client.isAdaptable(OpenShiftClient.class)) {
            OpenShiftClient openShiftClient = client.adapt(OpenShiftClient.class);
            ProjectRequest projectRequest = new ProjectRequestBuilder()
                    .withNewMetadata()
                    .withName(namespace)
                    .addToLabels(labelProvider.get().getLabels())
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
        KubernetesClient client = this.client.get();
        if (client.isAdaptable(OpenShiftClient.class)) {
            OpenShiftClient openShiftClient = client.adapt(OpenShiftClient.class);
            return openShiftClient.projects().withName(namespace).delete();
        } else {
            return super.delete(namespace);
        }
    }

    @Override
    public Boolean exists(String namespace) {
        KubernetesClient client = this.client.get();
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
    public void clean(String namespace) {
        KubernetesClient client = this.client.get();
        if (client.isAdaptable(OpenShiftClient.class)) {
            OpenShiftClient openShiftClient = client.adapt(OpenShiftClient.class);
            openShiftClient.deploymentConfigs().inNamespace(namespace).delete();
        }

        super.clean(namespace);
    }
}
