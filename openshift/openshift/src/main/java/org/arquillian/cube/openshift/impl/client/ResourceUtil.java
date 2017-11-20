package org.arquillian.cube.openshift.impl.client;

import io.fabric8.kubernetes.api.model.v3_1.Container;
import io.fabric8.kubernetes.api.model.v3_1.ContainerPort;
import io.fabric8.kubernetes.api.model.v3_1.Pod;
import io.fabric8.kubernetes.api.model.v3_1.PodCondition;
import io.fabric8.kubernetes.api.model.v3_1.PodStatus;
import io.fabric8.kubernetes.api.model.v3_1.Service;
import io.fabric8.kubernetes.api.model.v3_1.ServicePort;
import io.fabric8.kubernetes.clnt.v3_1.KubernetesClient;
import io.fabric8.kubernetes.clnt.v3_1.KubernetesClientException;
import io.fabric8.kubernetes.clnt.v3_1.Watch;
import io.fabric8.kubernetes.clnt.v3_1.Watcher;
import io.fabric8.openshift.api.model.v3_1.Build;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.arquillian.cube.spi.Binding;

public final class ResourceUtil {

    public static Pod waitForStart(KubernetesClient kubernetes, Pod resource) throws Exception {
        final AtomicReference<Pod> holder = new AtomicReference<Pod>();
        final CountDownLatch latch = new CountDownLatch(1);
        final Watcher<Pod> watcher = new Watcher<Pod>() {
            @Override
            public void eventReceived(Action action, Pod pod) {
                switch (action) {
                    case ADDED:
                    case MODIFIED:
                        if (pod.getStatus() != null && isRunning(pod.getStatus().getPhase()) && isReady(
                            pod.getStatus())) {
                            holder.compareAndSet(null, pod);
                            latch.countDown();
                        }
                        break;
                    case DELETED:
                    case ERROR:
                        System.err.println("Unexpected action waiting for pod to start: " + action);
                        holder.compareAndSet(null, pod);
                        latch.countDown();
                }
            }

            @Override
            public void onClose(KubernetesClientException cause) {
            }
        };

        System.out.print("waiting for pod " + resource.getMetadata().getName() + " ");
        Watch watch = kubernetes.pods()
            .inNamespace(resource.getMetadata().getNamespace())
            .withName(resource.getMetadata().getName())
            .watch(watcher);
        //TODO: use timeout
        latch.await();
        watch.close();
        System.out.println(" done!");
        return holder.get();
    }

    private static boolean isReady(PodStatus status) {
        for (PodCondition condition : status.getConditions()) {
            if ("Ready".equalsIgnoreCase(condition.getType()) && "False".equalsIgnoreCase(condition.getStatus())) {
                return false;
            }
        }
        return true;
    }

    public static Build waitForComplete(io.fabric8.openshift.clnt.v3_1.OpenShiftClient kubernetes, Build resource)
        throws Exception {
        final AtomicReference<Build> holder = new AtomicReference<Build>();
        final CountDownLatch latch = new CountDownLatch(1);
        final Watcher<Build> watcher = new Watcher<Build>() {
            @Override
            public void eventReceived(Action action, Build build) {
                switch (action) {
                    case ADDED:
                    case MODIFIED:
                        if (!("New".equals(build.getStatus().getPhase())
                            || "Pending".equals(build.getStatus().getPhase())
                            || "Running"
                            .equals(build.getStatus().getPhase()))) {
                            holder.compareAndSet(null, build);
                            latch.countDown();
                        }
                        break;
                    case DELETED:
                    case ERROR:
                        System.err.println("Unexpected action waiting for pod to start: " + action);
                        holder.compareAndSet(null, build);
                        latch.countDown();
                }
            }

            @Override
            public void onClose(KubernetesClientException cause) {
            }
        };

        System.out.print("waiting for build " + resource.getMetadata().getName() + " ");
        Watch watch = kubernetes.builds()
            .inNamespace(resource.getMetadata().getNamespace())
            .withName(resource.getMetadata().getName())
            .watch(watcher);
        //TODO: use timeout
        latch.await();
        watch.close();

        Build build = holder.get();
        if (isFailed(build) || !isComplete(build)) {
            System.out.println(" failed!");
            throw new RuntimeException("Build " + build.getMetadata().getName() + " failed. See log");
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
                binding.addPortBinding(port.getContainerPort(), port.getHostPort());
            }
        }
        return binding;
    }

    public static Binding toBinding(Service pod) {
        Binding binding = null;
        if (pod.getStatus() != null && pod.getSpec().getClusterIP() != null) { // Running
            // pod
            binding = new Binding(pod.getSpec().getClusterIP());
        } else { // Configured pod
            binding = new Binding(null);
        }
        for (ServicePort port : pod.getSpec().getPorts()) {
            // nodePort is service equivalent of pod's hostPort
            binding.addPortBinding(port.getPort(), port.getNodePort());
        }
        return binding;
    }
}
