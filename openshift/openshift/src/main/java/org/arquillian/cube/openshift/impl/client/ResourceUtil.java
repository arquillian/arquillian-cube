package org.arquillian.cube.openshift.impl.client;

import io.fabric8.kubernetes.api.model.v4_0.Container;
import io.fabric8.kubernetes.api.model.v4_0.ContainerPort;
import io.fabric8.kubernetes.api.model.v4_0.Pod;
import io.fabric8.kubernetes.api.model.v4_0.PodCondition;
import io.fabric8.kubernetes.api.model.v4_0.PodStatus;
import io.fabric8.kubernetes.api.model.v4_0.Service;
import io.fabric8.kubernetes.api.model.v4_0.ServicePort;
import io.fabric8.kubernetes.clnt.v4_0.KubernetesClient;
import io.fabric8.kubernetes.clnt.v4_0.KubernetesClientException;
import io.fabric8.kubernetes.clnt.v4_0.Watch;
import io.fabric8.kubernetes.clnt.v4_0.Watcher;
import io.fabric8.openshift.api.model.v4_0.Build;
import org.arquillian.cube.spi.Binding;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.awaitility.Awaitility.await;

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

    public static Build waitForComplete(io.fabric8.openshift.clnt.v4_0.OpenShiftClient kubernetes, Build resource)
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

    /**
     * Waits for the timeout duration until the url responds with correct status code
     *
     * @param routeUrl    URL to check (usually a route one)
     * @param timeout     Max timeout value to await for route readiness.
     *                    If not set, default timeout value is set to 5.
     * @param timeoutUnit TimeUnit used for timeout duration.
     *                    If not set, Minutes is used as default TimeUnit.
     * @param repetitions How many times in a row the route must respond successfully to be considered available.
     * @param statusCodes list of status code that might return that service is up and running.
     *                    It is used as OR, so if one returns true, then the route is considered valid.
     *                    If not set, then only 200 status code is used.
     */
    public static void awaitRoute(URL routeUrl, int timeout, TimeUnit timeoutUnit, int repetitions, int... statusCodes) {
        AtomicInteger successfulAwaitsInARow = new AtomicInteger(0);
        await().atMost(timeout, timeoutUnit).until(() -> {
            if (tryConnect(routeUrl, statusCodes)) {
                successfulAwaitsInARow.incrementAndGet();
            } else {
                successfulAwaitsInARow.set(0);
            }
            return successfulAwaitsInARow.get() >= repetitions;
        });
    }

    public static void awaitRoute(URL routeUrl, int timeout, TimeUnit timeoutUnit, int... statusCodes) {
        awaitRoute(routeUrl, timeout, timeoutUnit, 1, statusCodes);
    }

    public static void awaitRoute(URL routeUrl, int... statusCodes) {
        awaitRoute(routeUrl, 5, TimeUnit.MINUTES, 1, statusCodes);
    }

    private static boolean tryConnect(URL routeUrl, int[] statusCodes) {
        if (statusCodes.length == 0) {
            statusCodes = new int[]{200};
        }

        HttpURLConnection urlConnection = null;
        try {
            urlConnection = (HttpURLConnection) routeUrl.openConnection();
            urlConnection.setConnectTimeout(1000);
            urlConnection.setReadTimeout(1000);
            urlConnection.connect();
            int connectionResponseCode = urlConnection.getResponseCode();
            for (int expectedStatusCode : statusCodes) {
                if (expectedStatusCode == connectionResponseCode) {
                    return true;
                }
            }
        } catch (Exception e) {
            // retry
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        return false;
    }
}
