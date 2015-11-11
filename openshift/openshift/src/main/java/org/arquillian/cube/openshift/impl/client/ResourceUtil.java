package org.arquillian.cube.openshift.impl.client;

import org.arquillian.cube.spi.Binding;

import io.fabric8.kubernetes.api.Kubernetes;
import io.fabric8.kubernetes.api.KubernetesExtensions;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodCondition;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.openshift.api.model.Build;

public final class ResourceUtil {

    public static Pod waitForStart(Kubernetes kubernetes, Pod resource) throws Exception {
        Pod pod = resource;
        System.out.print("waiting for pod " + pod.getMetadata().getName() + " ");
        while (!isRunning(pod) || !isReady(pod.getStatus())) {
            System.out.print(".");
            Thread.sleep(200);
            pod = kubernetes.getPod(resource.getMetadata().getName(), resource.getMetadata().getNamespace());
        }
        System.out.println(" done!");
        return pod;
    }

    private static boolean isReady(PodStatus status) {
        for (PodCondition condition : status.getConditions()) {
            if ("Ready".equalsIgnoreCase(condition.getType()) && "False".equalsIgnoreCase(condition.getStatus())) {
                return false;
            }
        }
        return true;
    }

    public static Build waitForComplete(KubernetesExtensions kubernetes, Build resource) throws Exception {
        Build build = resource;
        System.out.print("waiting for build " + build.getMetadata().getName() + " ");
        while (!isComplete(build)) {
            if(isFailed(build)) {
                System.out.println(" failed!");
                throw new RuntimeException("Build " + build.getMetadata().getName() + " failed. See log");
            }
            System.out.print(".");
            Thread.sleep(200);
            build = kubernetes.getBuild(resource.getMetadata().getName(), resource.getMetadata().getNamespace());
        }
        System.out.println(" done!");
        return build;
    }

    public static boolean isRunning(Pod resource) throws Exception {
        return isRunning(resource.getStatus().getPhase());
    }

    public static boolean isComplete(Build resource) throws Exception {
        return isComplete(resource.getStatus().getPhase());
    }

    public static boolean isFailed(Build resource) throws Exception {
        return "Failed".equals(resource.getStatus().getPhase());
    }

    public static boolean isRunning(String phase) {
        return "Running".equals(phase);
    }

    public static boolean isComplete(String phase) {
        return "Complete".equals(phase);
    }

    public static Binding toBinding(Pod pod) {
        Binding binding = null;
        if (pod.getStatus() != null && pod.getStatus().getHostIP() != null) { // Running
                                                                              // pod
            binding = new Binding(pod.getStatus().getHostIP());
        } else { // Configured pod
            binding = new Binding(null);
        }
        for (Container container : pod.getSpec().getContainers()) {
            for (ContainerPort port : container.getPorts()) {
                binding.addPortBinding(port.getHostPort(), port.getContainerPort());
            }
        }
        return binding;
    }

    public static Binding toBinding(Service pod) {
        Binding binding = null;
        if (pod.getStatus() != null && pod.getSpec().getPortalIP() != null) { // Running
                                                                              // pod
            binding = new Binding(pod.getSpec().getPortalIP());
        } else { // Configured pod
            binding = new Binding(null);
        }
        for (ServicePort port : pod.getSpec().getPorts()) {
            binding.addPortBinding(port.getNodePort(), port.getTargetPort().getIntVal());
        }
        return binding;
    }
}
