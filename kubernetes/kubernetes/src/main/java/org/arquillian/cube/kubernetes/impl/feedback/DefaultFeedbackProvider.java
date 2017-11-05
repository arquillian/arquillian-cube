package org.arquillian.cube.kubernetes.impl.feedback;

import io.fabric8.kubernetes.api.model.v2_6.Container;
import io.fabric8.kubernetes.api.model.v2_6.Endpoints;
import io.fabric8.kubernetes.api.model.v2_6.Event;
import io.fabric8.kubernetes.api.model.v2_6.EventList;
import io.fabric8.kubernetes.api.model.v2_6.HasMetadata;
import io.fabric8.kubernetes.api.model.v2_6.LabelSelectorRequirement;
import io.fabric8.kubernetes.api.model.v2_6.Pod;
import io.fabric8.kubernetes.api.model.v2_6.PodList;
import io.fabric8.kubernetes.api.model.v2_6.PodListBuilder;
import io.fabric8.kubernetes.api.model.v2_6.ReplicationController;
import io.fabric8.kubernetes.api.model.v2_6.Service;
import io.fabric8.kubernetes.api.model.v2_6.extensions.Deployment;
import io.fabric8.kubernetes.api.model.v2_6.extensions.ReplicaSet;
import io.fabric8.kubernetes.clnt.v2_6.KubernetesClient;
import io.fabric8.kubernetes.clnt.v2_6.Watch;
import io.fabric8.kubernetes.clnt.v2_6.Watcher;
import io.fabric8.kubernetes.clnt.v2_6.dsl.FilterWatchListDeletable;
import java.util.HashMap;
import java.util.Map;
import org.arquillian.cube.kubernetes.api.FeedbackProvider;
import org.arquillian.cube.kubernetes.api.Logger;
import org.arquillian.cube.kubernetes.api.WithToImmutable;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;

public class DefaultFeedbackProvider implements FeedbackProvider {

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

    public static class ImmutableFeedbackProvider implements FeedbackProvider, WithToImmutable<FeedbackProvider> {

        @Inject
        protected final KubernetesClient client;

        @Inject
        protected final Logger logger;

        public ImmutableFeedbackProvider(KubernetesClient client, Logger logger) {
            this.client = client;
            this.logger = logger;
        }

        @Override
        public <T extends HasMetadata> void onResourceNotReady(T resource) {
            try {
                PodList podList = podsOf(resource);
                if (podList == null) {
                    return;
                }

                for (Pod pod : podList.getItems()) {
                    //That should only happen in tests.
                    if (pod.getSpec() == null || pod.getSpec().getContainers() == null) {
                        continue;
                    }

                    displayPodEvents(pod);

                    for (Container container : pod.getSpec().getContainers()) {
                        displayContainerLogs(pod, container);
                    }
                }
            } catch (Throwable t) {
                //ignore
            }
        }

        protected void displayContainerLogs(Pod pod, Container container) {
            try {
                logger.warn("Tailing logs of matching pod: ["
                    + pod.getMetadata().getName()
                    + "], container: ["
                    + container.getName()
                    + "]");
                logger.info(client.pods()
                    .inNamespace(pod.getMetadata().getNamespace())
                    .withName(pod.getMetadata().getName())
                    .inContainer(container.getName())
                    .tailingLines(100)
                    .withPrettyOutput()
                    .getLog());
            } catch (Throwable t) {
                logger.error("Failed to read logs, due to:" + t.getMessage());
            } finally {
                logger.warn("---");
            }
        }

        protected void displayPodEvents(Pod pod) {
            try {
                Map<String, String> fields = new HashMap<>();
                fields.put("involvedObject.uid", pod.getMetadata().getUid());
                fields.put("involvedObject.name", pod.getMetadata().getName());
                fields.put("involvedObject.namespace", pod.getMetadata().getNamespace());

                EventList eventList = client.events().inNamespace(pod.getMetadata().getNamespace()).withFields(fields).list();
                if (eventList == null) {
                    return;
                }
                logger.warn("Events of matching pod: [" + pod.getMetadata().getName() + "]");
                for (Event event : eventList.getItems()) {
                    logger.info(String.format("%s\t\t%s", event.getReason(), event.getMessage()));
                }
            } catch (Throwable t) {
                logger.error("Failed to read events, due to:" + t.getMessage());
            } finally {
                logger.warn("---");
            }
        }

        /**
         * Finds the pod that correspond to the specified resource.
         *
         * @param resource
         *     The resource.
         *
         * @return The podList with the matching pods.
         */
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
            } else {
                return new PodListBuilder().build();
            }
        }

        /**
         * Returns the {@link PodList} that match the specified {@link Deployment}.
         *
         * @param deployment
         *     The {@link Deployment}
         */
        public PodList findMatching(Deployment deployment) {
            FilterWatchListDeletable<Pod, PodList, Boolean, Watch, Watcher<Pod>> podLister =
                client.pods().inNamespace(deployment.getMetadata().getNamespace());
            if (deployment.getSpec().getSelector().getMatchLabels() != null) {
                podLister.withLabels(deployment.getSpec().getSelector().getMatchLabels());
            }
            if (deployment.getSpec().getSelector().getMatchExpressions() != null) {
                for (LabelSelectorRequirement req : deployment.getSpec().getSelector().getMatchExpressions()) {
                    switch (req.getOperator()) {
                        case "In":
                            podLister.withLabelIn(req.getKey(), req.getValues().toArray(new String[] {}));
                            break;
                        case "NotIn":
                            podLister.withLabelNotIn(req.getKey(), req.getValues().toArray(new String[] {}));
                            break;
                        case "DoesNotExist":
                            podLister.withoutLabel(req.getKey());
                            break;
                        case "Exists":
                            podLister.withLabel(req.getKey());
                            break;
                    }
                }
            }
            return podLister.list();
        }

        /**
         * Returns the {@link PodList} that match the specified {@link ReplicaSet}.
         *
         * @param replicaSet
         *     The {@link ReplicaSet}
         */
        public PodList findMatching(ReplicaSet replicaSet) {
            FilterWatchListDeletable<Pod, PodList, Boolean, Watch, Watcher<Pod>> podLister =
                client.pods().inNamespace(replicaSet.getMetadata().getNamespace());
            if (replicaSet.getSpec().getSelector().getMatchLabels() != null) {
                podLister.withLabels(replicaSet.getSpec().getSelector().getMatchLabels());
            }
            if (replicaSet.getSpec().getSelector().getMatchExpressions() != null) {
                for (LabelSelectorRequirement req : replicaSet.getSpec().getSelector().getMatchExpressions()) {
                    switch (req.getOperator()) {
                        case "In":
                            podLister.withLabelIn(req.getKey(), req.getValues().toArray(new String[] {}));
                            break;
                        case "NotIn":
                            podLister.withLabelNotIn(req.getKey(), req.getValues().toArray(new String[] {}));
                            break;
                        case "DoesNotExist":
                            podLister.withoutLabel(req.getKey());
                            break;
                        case "Exists":
                            podLister.withLabel(req.getKey());
                            break;
                    }
                }
            }
            return podLister.list();
        }

        @Override
        public FeedbackProvider toImmutable() {
            return this;
        }
    }
}
