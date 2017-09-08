package org.arquillian.cube.openshift.impl.model;

import io.fabric8.kubernetes.api.model.v2_6.Container;
import io.fabric8.kubernetes.api.model.v2_6.Pod;
import io.fabric8.openshift.api.model.v2_6.DeploymentConfig;
import java.util.ArrayList;
import java.util.List;

public interface Template<T> {

    T getTarget();

    List<TemplateImageRef> getRefs();

    void resolve(TemplateImageRef ref, String imageRef);

    public static abstract class ContainerTemplate<T> implements Template<T> {

        protected abstract List<Container> getContainers();

        @Override
        public List<Template.TemplateImageRef> getRefs() {
            List<TemplateImageRef> refs = new ArrayList<TemplateImageRef>();
            for (Container container : getContainers()) {
                String image = container.getImage();
                if (image != null && image.startsWith("arquillian:")) {
                    refs.add(new TemplateImageRef(image.replaceFirst("arquillian\\:", ""), container.getName()));
                }
            }
            return refs;
        }

        @Override
        public void resolve(TemplateImageRef ref, String imageRef) {
            for (Container container : getContainers()) {
                if (container.getName().equals(ref.getContainerName())) {
                    container.setImage(imageRef);
                }
            }
        }
    }

    public static class DeploymentConfigTemplate extends ContainerTemplate<DeploymentConfig> {
        private DeploymentConfig config;

        public DeploymentConfigTemplate(DeploymentConfig config) {
            this.config = config;
        }

        @Override
        public DeploymentConfig getTarget() {
            return config;
        }

        @Override
        protected List<Container> getContainers() {
            return config.getSpec().getTemplate().getSpec().getContainers();
        }
    }

    public static class PodTemplate extends ContainerTemplate<Pod> {
        private Pod pod;

        public PodTemplate(Pod pod) {
            this.pod = pod;
        }

        @Override
        public Pod getTarget() {
            return pod;
        }

        @Override
        protected List<Container> getContainers() {
            return pod.getSpec().getContainers();
        }
    }

    public static class TemplateImageRef {
        private String path;
        private String containerName;

        public TemplateImageRef(String path, String containerName) {
            this.path = path;
            this.containerName = containerName;
        }

        public String getContainerName() {
            return containerName;
        }

        public String getPath() {
            return path;
        }
    }
}
