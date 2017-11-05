package org.arquillian.cube.openshift.impl.client;

import io.fabric8.kubernetes.api.model.v2_6.HasMetadata;
import io.fabric8.kubernetes.api.model.v2_6.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.v2_6.KubernetesResource;
import io.fabric8.kubernetes.api.model.v2_6.Pod;
import io.fabric8.kubernetes.api.model.v2_6.PodBuilder;
import io.fabric8.kubernetes.api.model.v2_6.Service;
import io.fabric8.kubernetes.clnt.v2_6.Config;
import io.fabric8.openshift.api.model.v2_6.Build;
import io.fabric8.openshift.api.model.v2_6.BuildConfig;
import io.fabric8.openshift.api.model.v2_6.BuildConfigBuilder;
import io.fabric8.openshift.api.model.v2_6.BuildRequest;
import io.fabric8.openshift.api.model.v2_6.BuildRequestBuilder;
import io.fabric8.openshift.api.model.v2_6.ImageStream;
import io.fabric8.openshift.api.model.v2_6.ImageStreamBuilder;
import io.fabric8.openshift.clnt.v2_6.DefaultOpenShiftClient;
import io.fabric8.openshift.clnt.v2_6.NamespacedOpenShiftClient;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.arquillian.cube.openshift.impl.model.Template;
import org.arquillian.cube.openshift.impl.model.Template.TemplateImageRef;

import static org.arquillian.cube.openshift.impl.client.ResourceUtil.waitForStart;

public class OpenShiftClient {

    private String namespace;

    private NamespacedOpenShiftClient kubernetes;
    private GitServer gitserver;
    private boolean keepAliveGitServer;

    public OpenShiftClient(Config config, String namespace, boolean keepAliveGitServer) {
        this.kubernetes = new DefaultOpenShiftClient(config);
        this.namespace = namespace;
        this.keepAliveGitServer = keepAliveGitServer;
        this.gitserver = new GitServer(this.getClient(), config, namespace);
    }

    public List<Exception> clean(ResourceHolder holder) {
        List<Exception> exceptions = new ArrayList<Exception>();
        List<HasMetadata> resourcesToDelete = new ArrayList<HasMetadata>();
        for (KubernetesResource resource : holder.getResources()) {
            if (resource instanceof HasMetadata) {
                resourcesToDelete.add((HasMetadata) resource);
            }
        }
        try {
            getClient().lists().delete(new KubernetesListBuilder().withItems(resourcesToDelete).build());
        } catch (Exception e) {
            exceptions.add(e);
        }
        return exceptions;
    }

    public ResourceHolder build(Template<Pod> template) throws Exception {
        ResourceHolder holder = new ResourceHolder();
        Map<String, String> defaultLabels = getDefaultLabels();

        if (template.getRefs().size() == 0) {
            Pod service = createStartablePod(template, defaultLabels);
            holder.setPod(service);
            return holder;
        }

        for (TemplateImageRef ref : template.getRefs()) {
            URI repoUri = gitserver.push(new File(ref.getPath()), ref.getContainerName());

            String runID = ref.getContainerName();

            try {

                ImageStream is = new ImageStreamBuilder()
                    .withNewMetadata()
                    .withName(runID)
                    .withNamespace(namespace)
                    .withLabels(defaultLabels)
                    .endMetadata()
                    .build();
                is = getClientExt().imageStreams().createOrReplace(is);
                holder.addResource(is);

                BuildConfig config = new BuildConfigBuilder()
                    .withNewMetadata()
                    .withName(runID)
                    .withNamespace(namespace)
                    .withLabels(defaultLabels)
                    .endMetadata()
                    .withNewSpec()
                    .withNewSource()
                    .withNewGit()
                    .withUri(repoUri.toString())
                    .withRef("master")
                    .endGit()
                    .endSource()
                    .withNewStrategy()
                    .withType("Docker")
                    .withNewDockerStrategy()
                    .withNoCache(false)
                    .endDockerStrategy()
                    .endStrategy()
                    .withNewOutput()
                    .withNewTo()
                    .withKind("ImageStreamTag")
                    .withName(runID + ":latest")
                    .endTo()
                    .endOutput()
                    .endSpec()
                    .build();

                config = getClientExt().buildConfigs().createOrReplace(config);
                holder.addResource(config);

                final Long lastBuildVersion = config.getStatus().getLastVersion();
                BuildRequest br = new BuildRequestBuilder()
                    .withNewMetadata()
                    .withName(config.getMetadata().getName())
                    .withLabels(defaultLabels)
                    .endMetadata()
                    .build();

                getClientExt().buildConfigs().inNamespace(namespace).withName(runID).instantiate(br);
                Build build = ResourceUtil.waitForComplete(
                    getClientExt(),
                    getClientExt().builds().inNamespace(namespace)
                        .withName(String.format("%s-%d", config.getMetadata().getName(), (lastBuildVersion + 1)))
                        .get());

                holder.addResource(build);

                is = getClientExt().imageStreams().inNamespace(namespace).withName(is.getMetadata().getName()).get();

                String imageRef = is.getStatus().getTags().get(0).getItems().get(0).getDockerImageReference();
                template.resolve(ref, imageRef);

                Pod service = createStartablePod(template, defaultLabels);
                holder.setPod(service);
            } catch (Exception e) {
                holder.setException(e);
            }
        }
        return holder;
    }

    private Pod createStartablePod(Template<Pod> template, Map<String, String> defaultLabels) {
        Map<String, String> allLabels = new HashMap<String, String>();
        allLabels.putAll(defaultLabels);
        allLabels.putAll(template.getTarget().getMetadata().getLabels());
        Pod service = new PodBuilder()
            .withNewMetadataLike(template.getTarget().getMetadata())
            .withLabels(allLabels)
            .endMetadata()
            .withNewSpecLike(template.getTarget().getSpec())
            .endSpec()
            .build();
        return service;
    }

    public Pod createAndWait(Pod resource) throws Exception {
        return waitForStart(
            getClient(),
            getClient().pods().inNamespace(namespace).create(resource));
    }

    public Service create(Service resource) throws Exception {
        return (Service) getClient().services().inNamespace(namespace).create(resource);
    }

    public void destroy(Pod resource) throws Exception {
        getClient().pods().inNamespace(namespace).withName(resource.getMetadata().getName()).delete();
    }

    public void destroy(Service resource) throws Exception {
        getClient().services().inNamespace(namespace).withName(resource.getMetadata().getName()).delete();
    }

    public Pod update(Pod resource) throws Exception {
        return getClient().resource(resource).createOrReplace();
    }

    public void shutdown() throws Exception {
        if (!keepAliveGitServer) {
            gitserver.shutdown();
        }
    }

    public NamespacedOpenShiftClient getClient() {
        return kubernetes;
    }

    public io.fabric8.openshift.clnt.v2_6.OpenShiftClient getClientExt() {
        return kubernetes;
    }

    private Map<String, String> getDefaultLabels() {
        Map<String, String> labels = new HashMap<String, String>();
        labels.put("generatedby", "arquillian");
        return labels;
    }

    public static class ResourceHolder {

        public Pod pod;
        public Set<KubernetesResource> resources;
        private Exception exception;

        public ResourceHolder() {
            this(null);
        }

        public ResourceHolder(Pod pod) {
            this.pod = pod;
            this.resources = new HashSet<KubernetesResource>();
        }

        public Exception getException() {
            return exception;
        }

        public void setException(Exception exception) {
            this.exception = exception;
        }

        public void addResource(KubernetesResource resource) {
            this.resources.add(resource);
        }

        public Set<KubernetesResource> getResources() {
            if (resources == null) {
                return new HashSet<KubernetesResource>();
            }
            return resources;
        }

        public Pod getPod() {
            return pod;
        }

        public void setPod(Pod pod) {
            this.pod = pod;
        }
    }
}
