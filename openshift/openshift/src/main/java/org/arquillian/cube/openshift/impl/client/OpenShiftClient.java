package org.arquillian.cube.openshift.impl.client;

import static io.fabric8.kubernetes.api.KubernetesHelper.loadJson;
import static org.arquillian.cube.openshift.impl.client.ResourceUtil.waitForStart;

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

import io.fabric8.kubernetes.api.Kubernetes;
import io.fabric8.kubernetes.api.KubernetesExtensions;
import io.fabric8.kubernetes.api.KubernetesFactory;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.openshift.api.model.Build;
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.BuildConfigBuilder;
import io.fabric8.openshift.api.model.BuildRequest;
import io.fabric8.openshift.api.model.BuildRequestBuilder;
import io.fabric8.openshift.api.model.ImageStream;
import io.fabric8.openshift.api.model.ImageStreamBuilder;

public class OpenShiftClient {

    private KubernetesFactory factory;
    private String namespace;

    private Kubernetes kubernetes;
    private KubernetesExtensions kubernetesExtensions;
    private GitServer gitserver;
    private boolean keepAliveGitServer;

    public OpenShiftClient(KubernetesFactory factory, String namespace, boolean keepAliveGitServer) {
        this.factory = factory;
        this.namespace = namespace;
        this.keepAliveGitServer = keepAliveGitServer;
        this.gitserver = new GitServer(this.getClient(), namespace);
    }

    public List<Exception> clean(ResourceHolder holder) {

        List<Exception> exceptions = new ArrayList<Exception>();

        for (KubernetesResource resource : holder.getResources()) {
            try {
                if (resource instanceof Pod) {
                    Pod m = (Pod) resource;
                    getClient().deletePod(m.getMetadata().getName(), namespace);
                } else if (resource instanceof ImageStream) {
                    ImageStream m = (ImageStream) resource;
                    getClientExt().deleteImageStream(m.getMetadata().getName(), namespace);
                } else if (resource instanceof BuildConfig) {
                    BuildConfig m = (BuildConfig) resource;
                    getClientExt().deleteBuildConfig(m.getMetadata().getName(), namespace);
                } else if (resource instanceof Build) {
                    Build build = (Build) resource;
                    getClientExt().deleteBuild(build.getMetadata().getName(), namespace);
                }
            } catch (Exception e) {
                exceptions.add(e);
            }
        }
        return exceptions;
    }

	public ResourceHolder build(Template<Pod> template) throws Exception {
        ResourceHolder holder = new ResourceHolder();
        Map<String, String> defaultLabels = getDefaultLabels();

        if(template.getRefs().size() == 0) {
            Pod service = createStartablePod(template, defaultLabels);
            holder.setPod(service);
            return holder;
        }
        
        for(TemplateImageRef ref : template.getRefs()) {
	        URI repoUri = gitserver.push(new File(ref.getPath()), ref.getContainerName());

	        String runID = ref.getContainerName();

	        try {
	            ImageStream is = new ImageStreamBuilder()
	                    .withNewMetadata()
	                        .withName(runID)
	                        .withLabels(defaultLabels)
	                        .endMetadata()
	                    .build();
	            is = (ImageStream)KubernetesHelper.loadJson(getClientExt().createImageStream(is, namespace));
	            holder.addResource(is);

	            BuildConfig config = new BuildConfigBuilder()
	                    .withNewMetadata()
	                        .withName(runID)
	                        .withLabels(defaultLabels)
	                        .endMetadata()
	                    .withNewSpec()
	                        .withNewSource()
	                            .withNewGit("master", repoUri.toString())
	                            .withType("Git")
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

	            config = (BuildConfig)KubernetesHelper.loadJson(getClientExt().createBuildConfig(config, namespace));
	            holder.addResource(config);

	            BuildRequest br = new BuildRequestBuilder()
	                    .withNewMetadata()
	                        .withName(config.getMetadata().getName())
	                        .withLabels(defaultLabels)
	                        .endMetadata()
	                    .build();

	            Build build = ResourceUtil.waitForComplete(
	                    getClientExt(),
	                    (Build)KubernetesHelper.loadJson(
	                            getClientExt().instantiateBuild(
	                                    config.getMetadata().getName(), br, namespace)));

	            holder.addResource(build);

	            is = getClientExt().getImageStream(is.getMetadata().getName(), namespace);

	            String imageRef = is.getStatus().getTags().get(0).getItems().get(0).getDockerImageReference();
	            template.resolve(ref,  imageRef);

                Pod service = createStartablePod(template, defaultLabels);
	            holder.setPod(service);
	        } catch(Exception e) {
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
				(Pod)loadJson(
						getClient().createPod(resource, namespace)));
	}

    public Service create(Service resource) throws Exception {
        return (Service)loadJson(getClient().createService(resource, namespace));
    }

    public void destroy(Pod resource) throws Exception {
		getClient().deletePod(resource.getMetadata().getName(), namespace);
	}

    public void destroy(Service resource) throws Exception {
        getClient().deleteService(resource.getMetadata().getName(), namespace);
    }

    public Pod update(Pod resource) throws Exception {
		return getClient().getPod(resource.getMetadata().getName(), namespace);
	}

	public void shutdown() throws Exception {
		if(!keepAliveGitServer) {
			gitserver.shutdown();
		}
	}

	public Kubernetes getClient() {
		if(kubernetes == null) {
			kubernetes = factory.createKubernetes();
		}
		return kubernetes;
	}

	public KubernetesExtensions getClientExt() {
		if(kubernetesExtensions == null) {
			kubernetesExtensions = factory.createKubernetesExtensions();
		}
		return kubernetesExtensions;
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

        public void setException(Exception exception) {
            this.exception = exception;
        }

        public Exception getException() {
            return exception;
        }

        public void setPod(Pod pod) {
            this.pod = pod;
        }

        public void addResource(KubernetesResource resource) {
            this.resources.add(resource);
        }

        public Set<KubernetesResource> getResources() {
            if(resources == null) {
                return new HashSet<KubernetesResource>();
            }
            return resources;
        }

        public Pod getPod() {
            return pod;
        }
    }
}
