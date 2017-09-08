package org.arquillian.cube.openshift.impl.feedback;

import io.fabric8.kubernetes.api.model.v2_6.Endpoints;
import io.fabric8.kubernetes.api.model.v2_6.HasMetadata;
import io.fabric8.kubernetes.api.model.v2_6.Pod;
import io.fabric8.kubernetes.api.model.v2_6.PodList;
import io.fabric8.kubernetes.api.model.v2_6.PodListBuilder;
import io.fabric8.kubernetes.api.model.v2_6.ReplicationController;
import io.fabric8.kubernetes.api.model.v2_6.Service;
import io.fabric8.kubernetes.api.model.v2_6.extensions.Deployment;
import io.fabric8.kubernetes.api.model.v2_6.extensions.ReplicaSet;
import io.fabric8.kubernetes.clnt.v2_6.KubernetesClient;
import io.fabric8.openshift.api.model.v2_6.DeploymentConfig;
import org.arquillian.cube.kubernetes.api.FeedbackProvider;
import org.arquillian.cube.kubernetes.api.Logger;
import org.arquillian.cube.kubernetes.api.WithToImmutable;
import org.arquillian.cube.kubernetes.impl.feedback.DefaultFeedbackProvider;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;

public class OpenshiftFeedbackProvider implements FeedbackProvider {

    protected FeedbackProvider delegate;

    @Inject
    protected Instance<KubernetesClient> client;

    @Inject
    protected Instance<Logger> logger;

    @Override
    public <T extends HasMetadata> void onResourceNotReady(T resource) {

    }

    @Override
    public FeedbackProvider toImmutable() {
        if (delegate != null) {
            return delegate;
        }
        synchronized (this) {
            if (delegate == null) {
                delegate = new ImmutableFeedbackProvider(client.get(),
                    logger.get().toImmutable());
            }
        }
        return delegate;
    }

    public static class ImmutableFeedbackProvider extends DefaultFeedbackProvider.ImmutableFeedbackProvider
        implements FeedbackProvider, WithToImmutable<FeedbackProvider> {

        public ImmutableFeedbackProvider(KubernetesClient client, Logger logger) {
            super(client, logger);
        }

        public <T extends HasMetadata> PodList podsOf(T resource) {
            if (resource instanceof Pod) {
                return new PodListBuilder().withItems((Pod) resource).build();
            } else if (resource instanceof Endpoints) {
                return podsOf(client.services()
                    .inNamespace(resource.getMetadata().getNamespace())
                    .withName(resource.getMetadata().getName())
                    .get());
            } else if (resource instanceof Service) {
                return client.pods()
                    .inNamespace(resource.getMetadata().getNamespace())
                    .withLabels(((Service) resource).getSpec().getSelector())
                    .list();
            } else if (resource instanceof ReplicationController) {
                return client.pods()
                    .inNamespace(resource.getMetadata().getNamespace())
                    .withLabels(((ReplicationController) resource).getSpec().getSelector())
                    .list();
            } else if (resource instanceof ReplicaSet) {
                return findMatching((ReplicaSet) resource);
            } else if (resource instanceof Deployment) {
                return findMatching((Deployment) resource);
            } else if (resource instanceof DeploymentConfig) {
                return client.pods().inNamespace(resource.getMetadata().getName()).withLabel("deploymentconfig",
                    resource.getMetadata().getName()).list();
            } else {
                return new PodListBuilder().build();
            }
        }

        @Override
        public FeedbackProvider toImmutable() {
            return this;
        }
    }
}
