package org.arquillian.cube.openshift.impl.client;

import io.fabric8.kubernetes.api.model.v3_1.HasMetadata;
import io.fabric8.kubernetes.api.model.v3_1.Pod;
import io.fabric8.kubernetes.clnt.v3_1.KubernetesClientException;
import io.fabric8.kubernetes.clnt.v3_1.dsl.NamespaceListVisitFromServerGetDeleteRecreateWaitApplicable;
import io.fabric8.kubernetes.clnt.v3_1.internal.readiness.Readiness;
import io.fabric8.openshift.api.model.v3_1.DeploymentConfig;
import io.fabric8.openshift.api.model.v3_1.Route;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.awaitility.Awaitility.await;

/**
 * Class that allows you to deploy undeploy and wait for resources programmatically in a test.
 *
 */
public class OpenShiftAssistant {

    private static final Logger log = Logger.getLogger(OpenShiftAssistant.class.getName());
    private final io.fabric8.openshift.clnt.v3_1.OpenShiftClient client;

    private final String namespace;

    private String applicationName;
    private OpenShiftAssistantDefaultResourcesLocator openShiftAssistantDefaultResourcesLocator;

    private Map<String, List<HasMetadata>> created
        = new LinkedHashMap<>();

    public OpenShiftAssistant(io.fabric8.openshift.clnt.v3_1.OpenShiftClient openShiftClient, String namespace) {
        this.client = openShiftClient;
        this.namespace = namespace;
        this.openShiftAssistantDefaultResourcesLocator = new OpenShiftAssistantDefaultResourcesLocator();
    }

    /**
     * Deploys application finding resources in default location in classpath. That is:
     * openshift.(y[a]ml|json), kubernetes.(y[a]ml|json), META-INF/fabric8/openshift.(y[a]ml|json), META-INF/fabric8/kubernetes.(y[a]ml|json)
     * @return the name of the application defined in the DeplymentConfig.
     * @throws IOException
     */
    public String deployApplication() throws IOException {
        return deployApplication(null);
    }

    /**
     * Deploys application finding resources in default location in classpath. That is:
     * openshift.(y[a]ml|json), kubernetes.(y[a]ml|json), META-INF/fabric8/openshift.(y[a]ml|json), META-INF/fabric8/kubernetes.(y[a]ml|json)
     *
     * In this method yo specify the application name.
     *
     * @param applicationName to configure in cluster
     * @return the name of the application
     * @throws IOException
     */
    public String deployApplication(String applicationName) throws IOException {

        final Optional<URL> defaultFileOptional = this.openShiftAssistantDefaultResourcesLocator.locate();

        if (defaultFileOptional.isPresent()) {
            deployApplication(applicationName, defaultFileOptional.get());
        } else {
            log.warning("No default Kubernetes or OpenShift resources found at default locations.");
        }

        return this.applicationName;
    }

    /**
     * Deploys application reading resources from specified classpath location
     * @param applicationName to configure in cluster
     * @param classpathLocations where resources are read
     * @return the name of the application
     * @throws IOException
     */
    public String deployApplication(String applicationName, String... classpathLocations) throws IOException {

        final List<URL> classpathElements = Arrays.stream(classpathLocations)
            .map(classpath -> Thread.currentThread().getContextClassLoader().getResource(classpath))
            .collect(Collectors.toList());

        deployApplication(applicationName, classpathElements.toArray(new URL[classpathElements.size()]));

        return this.applicationName;
    }

    /**
     * Deploys application reading resources from specified URLs
     * @param applicationName to configure in cluster
     * @param urls where resources are read
     * @return the name of the application
     * @throws IOException
     */
    public String deployApplication(String applicationName, URL... urls) throws IOException {
        this.applicationName = applicationName;

        for (URL url : urls) {
            try (InputStream inputStream = url.openStream()) {
                deploy(inputStream);
            }
        }

        return this.applicationName;
    }

    private void deploy(InputStream inputStream) throws IOException {
        final List<? extends HasMetadata> entities
            = deploy("application", inputStream);

        if (this.applicationName == null) {

            Optional<String> deploymentConfig = entities.stream()
                .filter(hm -> hm instanceof DeploymentConfig)
                .map(hm -> (DeploymentConfig) hm)
                .map(dc -> dc.getMetadata().getName()).findFirst();

            deploymentConfig.ifPresent(name -> this.applicationName = name);
        }
    }

    private List<? extends HasMetadata> deploy(String name, InputStream element) throws IOException {
        NamespaceListVisitFromServerGetDeleteRecreateWaitApplicable<HasMetadata, Boolean> declarations = client.load(element);
        List<HasMetadata> entities = declarations.createOrReplace();

        this.created.merge(name, entities, (list1, list2) -> Stream.of(list1, list2)
            .flatMap(Collection::stream)
            .collect(Collectors.toList()));

        log.info(String.format("%s deployed, %s object(s) created.", name, entities.size()));

        return entities;
    }

    /**
     * Gets the URL of the route with given name.
     * @param routeName to return its URL
     * @return URL backed by the route with given name.
     */
    public Optional<URL> getRoute(String routeName) {
        Route route = client.routes()
            .inNamespace(namespace).withName(applicationName).get();

        return route != null ? createUrlFromRoute(route) : Optional.empty();
    }

    /**
     * Returns the URL of the first route.
     * @return URL backed by the first route.
     */
    public Optional<URL> getRoute() {
        Optional<Route> optionalRoute = client.routes().inNamespace(namespace)
            .list().getItems()
            .stream()
            .findFirst();

        return optionalRoute
            .map(this::createUrlFromRoute)
            .orElse(Optional.empty());
    }

    private Optional<URL> createUrlFromRoute(Route route) {
        try {
            final String protocol = route.getSpec().getTls() == null ? "http" : "https";
            final String path = route.getSpec().getPath() == null ? "" : route.getSpec().getPath();
            return Optional.of(
                new URL(protocol, route.getSpec().getHost(), resolvePort(protocol), path));
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    private int resolvePort(String protocol) {
        if ("http".equals(protocol)) {
            return 80;
        } else {
            return 443;
        }
    }

    /**
     * Removes all resources deployed using this class.
     */
    public void cleanup() {
        List<String> keys = new ArrayList<>(created.keySet());
        keys.sort(String::compareTo);
        for (String key : keys) {
            created.remove(key)
                .stream()
                .sorted(Comparator.comparing(HasMetadata::getKind))
                .forEach(metadata -> {
                    log.info(String.format("Deleting %s : %s", key, metadata.getKind()));
                    deleteWithRetries(metadata);
                });
        }
    }

    private void deleteWithRetries(HasMetadata metadata) {
        int retryCounter = 0;
        boolean deleteUnsucessful = true;
        do {
            retryCounter++;
            try {
                // returns false when successfully deleted
                deleteUnsucessful = client.resource(metadata).withGracePeriod(0).delete();
            } catch (KubernetesClientException e) {
                try {
                    TimeUnit.MILLISECONDS.sleep(500);
                } catch (InterruptedException interrupted) {
                    throw new RuntimeException(interrupted);
                }
                e.printStackTrace();
                log.info(String.format("Error deleting resource %s %s retrying #%s ", metadata.getKind(),
                    metadata.getMetadata().getName(), retryCounter));
            }
        } while (retryCounter < 3 && deleteUnsucessful);
        if (deleteUnsucessful) {
            throw new RuntimeException("Unable to delete " + metadata);
        }
    }

    /**
     * Awaits at most 5 minutes until all pods are running.
     */
    public void awaitApplicationReadinessOrFail() {
        await().atMost(5, TimeUnit.MINUTES).until(() -> {
                List<Pod> list = client.pods().inNamespace(namespace).list().getItems();
                return list.stream()
                    .filter(pod -> pod.getMetadata().getName().startsWith(applicationName))
                    .filter(this::isRunning)
                    .collect(Collectors.toList()).size() >= 1;
            }
        );
    }

    private boolean isRunning(Pod pod) {
        return "running".equalsIgnoreCase(pod.getStatus().getPhase());
    }

    public String project() {
        return namespace;
    }

    /**
     * Awaits at most 5 minutes until all pods meets the given predicate.
     * @param filter used to wait to detect that a pod is up and running.
     */
    public void awaitPodReadinessOrFail(Predicate<Pod> filter) {
        await().atMost(5, TimeUnit.MINUTES).until(() -> {
                List<Pod> list = client.pods().inNamespace(namespace).list().getItems();
                return list.stream()
                    .filter(filter)
                    .filter(this::isRunning)
                    .collect(Collectors.toList()).size() >= 1;
            }
        );
    }

    /**
     * Scaling the application to given replicas
     * @param replicas to scale the application
     */
    public void scale(final int replicas) {
        log.info(String.format("Scaling replicas from %s to %s.", getPods("deploymentconfig").size(), replicas));
        this.client
            .deploymentConfigs()
            .inNamespace(this.namespace)
            .withName(this.applicationName)
            .scale(replicas);

        // ideally, we'd look at deployment config's status.availableReplicas field,
        // but that's only available since OpenShift 3.5
        await().atMost(5, TimeUnit.MINUTES)
            .until(() -> {
                final List<Pod> pods = getPods("deploymentconfig");
                try {
                    return pods.size() == replicas && pods.stream()
                        .allMatch(Readiness::isPodReady);
                } catch (final IllegalStateException e) {
                    // the 'Ready' condition can be missing sometimes, in which case Readiness.isPodReady throws an exception
                    // here, we'll swallow that exception in hope that the 'Ready' condition will appear later
                    return false;
                }
            });
    }

    private List<Pod> getPods(String label) {
        return this.client
            .pods()
            .inNamespace(this.namespace)
            .withLabel(label, this.applicationName)
            .list()
            .getItems();
    }

    /**
     * Method that returns the current deployment configuration object
     * @return Current deployment config object.
     */
    public DeploymentConfig deploymentConfig() {
        return this.client
            .deploymentConfigs()
            .inNamespace(this.namespace)
            .withName(this.applicationName)
            .get();
    }
}
