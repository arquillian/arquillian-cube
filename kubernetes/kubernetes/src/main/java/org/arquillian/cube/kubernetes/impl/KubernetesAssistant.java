package org.arquillian.cube.kubernetes.impl;

import io.fabric8.kubernetes.api.model.v3_1.EndpointSubset;
import io.fabric8.kubernetes.api.model.v3_1.Endpoints;
import io.fabric8.kubernetes.api.model.v3_1.HasMetadata;
import io.fabric8.kubernetes.api.model.v3_1.Pod;
import io.fabric8.kubernetes.api.model.v3_1.ReplicationController;
import io.fabric8.kubernetes.api.model.v3_1.Service;
import io.fabric8.kubernetes.api.model.v3_1.ServicePort;
import io.fabric8.kubernetes.api.model.v3_1.extensions.Deployment;
import io.fabric8.kubernetes.clnt.v3_1.ConfigBuilder;
import io.fabric8.kubernetes.clnt.v3_1.KubernetesClient;
import io.fabric8.kubernetes.clnt.v3_1.KubernetesClientException;
import io.fabric8.kubernetes.clnt.v3_1.dsl.NamespaceListVisitFromServerGetDeleteRecreateWaitApplicable;
import io.fabric8.kubernetes.clnt.v3_1.internal.readiness.Readiness;
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.github.lukehutch.fastclasspathscanner.matchprocessor.FileMatchProcessor;
import org.arquillian.cube.kubernetes.impl.portforward.PortForwarder;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.arquillian.cube.kubernetes.impl.utils.ResourceFilter;

import static org.arquillian.cube.kubernetes.impl.enricher.KuberntesServiceUrlResourceProvider.LOCALHOST;
import static org.awaitility.Awaitility.await;

/**
 * Class that allows you to deploy undeploy and wait for resources programmatically in a test.
 */
public class KubernetesAssistant {

    private static final Logger log = Logger.getLogger(KubernetesAssistant.class.getName());


    protected KubernetesClient client;
    protected String namespace;
    protected String applicationName;

    private KubernetesAssistantDefaultResourceLocator kubernetesAssistantDefaultResourcesLocator;
    private Map<String, List<HasMetadata>> created = new LinkedHashMap<>();

    public KubernetesAssistant(KubernetesClient client, String namespace) {
        this.client = client;
        this.namespace = namespace;
        this.kubernetesAssistantDefaultResourcesLocator = new KubernetesAssistantDefaultResourceLocator();
    }

    /**
     * Deploys application finding resources in default location in classpath. That is:
     * kubernetes.(y[a]ml|json), META-INF/fabric8/kubernetes.(y[a]ml|json)
     *
     * @return the name of the application defined in the Deployment.
     * @throws IOException
     */
    public String deployApplication() throws IOException {
        deployApplication((String) null);
        return this.applicationName;
    }

    /**
     * Deploys application finding resources in default location in classpath. That is:
     * kubernetes.(y[a]ml|json), META-INF/fabric8/kubernetes.(y[a]ml|json)
     * <p>
     * In this method you specify the application name.
     *
     * @param applicationName to configure in cluster
     * @return the name of the application
     * @throws IOException
     */
    public void deployApplication(String applicationName) throws IOException {

        final Optional<URL> defaultFileOptional = this.kubernetesAssistantDefaultResourcesLocator.locate();

        if (defaultFileOptional.isPresent()) {
            deployApplication(applicationName, defaultFileOptional.get());
        } else {
            log.warning("No default Kubernetes resources found at default locations.");
        }
    }

    /**
     * Deploys application reading resources from specified classpath location
     *
     * @param applicationName    to configure in cluster
     * @param classpathLocations where resources are read
     * @throws IOException
     */
    public void deployApplication(String applicationName, String... classpathLocations) throws IOException {

        final List<URL> classpathElements = Arrays.stream(classpathLocations)
            .map(classpath -> Thread.currentThread().getContextClassLoader().getResource(classpath))
            .collect(Collectors.toList());

        deployApplication(applicationName, classpathElements.toArray(new URL[classpathElements.size()]));
    }

    /**
     * Deploys application reading resources from specified URLs
     *
     * @param urls where resources are read
     * @return the name of the application
     * @throws IOException
     */
    public String deployApplication(URL... urls) throws IOException {
        deployApplication(null, urls);
        return this.applicationName;
    }

    /**
     * Deploys application reading resources from specified URLs
     *
     * @param applicationName to configure in cluster
     * @param urls            where resources are read
     * @return the name of the application
     * @throws IOException
     */
    public void deployApplication(String applicationName, URL... urls) throws IOException {
        this.applicationName = applicationName;

        for (URL url : urls) {
            try (InputStream inputStream = url.openStream()) {
                deploy(inputStream);
            }
        }
    }

    /**
     * Deploys application reading resources from classpath, matching the given regular expression.
     * For example kubernetes/.*\\.json will deploy all resources ending with json placed at kubernetes classpath directory.
     *
     * @param applicationName to configure the cluster
     * @param pattern         to match the resources.
     */
    public void deployAll(String applicationName, String pattern) {
        this.applicationName = applicationName;

        final FastClasspathScanner fastClasspathScanner = new FastClasspathScanner();

        fastClasspathScanner.matchFilenamePattern(pattern, (FileMatchProcessor) (relativePath, inputStream, lengthBytes) -> {
            deploy(inputStream);
        }).scan();
    }

    /**
     * Deploys application reading resources from classpath, matching the given regular expression.
     * For example kubernetes/.*\\.json will deploy all resources ending with json placed at kubernetes classpath directory.
     *
     * @param pattern to match the resources.
     */
    public String deployAll(String pattern) {
        final FastClasspathScanner fastClasspathScanner = new FastClasspathScanner();

        fastClasspathScanner.matchFilenamePattern(pattern, (FileMatchProcessor) (relativePath, inputStream, lengthBytes) -> {
            deploy(inputStream);
            inputStream.close();
        }).scan();

        return this.applicationName;
    }

    /**
     * Deploys all y(a)ml and json files located at given directory.
     *
     * @param directory where resource files are stored
     * @return the name of the application
     * @throws IOException
     */
    public String deployAll(Path directory) throws IOException {
        deployAll(null, directory);
        return this.applicationName;
    }

    /**
     * Deploys all y(a)ml and json files located at given directory.
     *
     * @param applicationName to configure in cluster
     * @param directory       where resources files are stored
     * @throws IOException
     */
    public void deployAll(String applicationName, Path directory) throws IOException {
        this.applicationName = applicationName;

        if (Files.isDirectory(directory)) {
            Files.list(directory)
                .filter(ResourceFilter::filterKubernetesResource)
                .map(p -> {
                    try {
                        return Files.newInputStream(p);
                    } catch (IOException e) {
                        throw new IllegalArgumentException(e);
                    }
                })
                .forEach(is -> {
                    try {
                        deploy(is);
                        is.close();
                    } catch (IOException e) {
                        throw new IllegalArgumentException(e);
                    }
                });
        } else {
            throw new IllegalArgumentException(String.format("%s should be a directory", directory));
        }
    }

    /**
     * Deploys application reading resources from specified InputStream
     *
     * @param inputStream  where resources are read
     * @throws IOException
     */
    public void deploy(InputStream inputStream) throws IOException {
        final List<? extends HasMetadata> entities = deploy("application", inputStream);

        if (this.applicationName == null) {

            Optional<String> deployment = entities.stream()
                .filter(hm -> hm instanceof Deployment)
                .map(hm -> (Deployment) hm)
                .map(rc -> rc.getMetadata().getName()).findFirst();

            deployment.ifPresent(name -> this.applicationName = name);
        }
    }

    protected List<? extends HasMetadata> deploy(String name, InputStream element) {
        NamespaceListVisitFromServerGetDeleteRecreateWaitApplicable<HasMetadata, Boolean> declarations = client.load(element);
        List<HasMetadata> entities = declarations.createOrReplace();

        this.created.merge(name, entities, (list1, list2) -> Stream.of(list1, list2)
            .flatMap(Collection::stream)
            .collect(Collectors.toList()));

        log.info(String.format("%s deployed, %s object(s) created.", name, entities.size()));

        return entities;
    }
    /**
     * Gets the URL of the service with the given name that has been created during the current session.
     *
     * @param name to return its URL
     * @return URL of the service.
     */
    public Optional<URL> getServiceUrl(String name) {
        Service service = client.services().inNamespace(namespace).withName(name).get();
        return service != null ? createUrlForService(service) : Optional.empty();
    }

    /**
     * Gets the URL of the first service that have been created during the current session.
     *
     * @return URL of the first service.
     */
    public Optional<URL> getServiceUrl() {
        Optional<Service> optionalService = client.services().inNamespace(namespace)
            .list().getItems()
            .stream()
            .findFirst();

        return optionalService
            .map(this::createUrlForService)
            .orElse(Optional.empty());
    }

    private Optional<URL> createUrlForService(Service service) {
        final String scheme = (service.getMetadata() != null && service.getMetadata().getAnnotations() != null) ?
            service.getMetadata().getAnnotations().get("api.service.kubernetes.io/scheme") : "http";
        final String path = (service.getMetadata() != null && service.getMetadata().getAnnotations() != null) ?
            service.getMetadata().getAnnotations().get("api.service.kubernetes.io/path") : "/";
        final int port = resolvePort(service);

        try {
            if (port > 0) {
                return Optional.of(new URL(scheme, LOCALHOST, port, path));
            } else {
                return Optional.of(new URL(scheme, LOCALHOST, path));
            }
        } catch (MalformedURLException e) {
            throw new IllegalStateException(
                "Cannot resolve URL for service: [" + service.getMetadata().getName() + "] in namespace:[" + namespace + "].");
        }
    }

    private int resolvePort(Service service) {
        final Pod pod = getRandomPod(client, service.getMetadata().getName(), namespace);
        final ServicePort servicePort = (service.getSpec() != null && service.getSpec().getPorts() != null) ?
            service.getSpec().getPorts().get(0) : null;
        final int containerPort = servicePort != null ? servicePort.getTargetPort().getIntVal() : 0;

        return portForward(pod.getMetadata().getName(), containerPort, namespace);
    }

    private int portForward(String podName, int targetPort, String namespace) {
        return portForward(podName, findRandomFreeLocalPort(), targetPort, namespace);
    }

    private int portForward(String podName, int sourcePort, int targetPort, String namespace) {
        try {
            final io.fabric8.kubernetes.clnt.v3_1.Config build = new ConfigBuilder(client.getConfiguration()).withNamespace(namespace).build();
            final PortForwarder portForwarder = new PortForwarder(build, podName);
            portForwarder.forwardPort(sourcePort, targetPort);
            return sourcePort;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static int findRandomFreeLocalPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Pod getRandomPod(KubernetesClient client, String name, String namespace) {
        Endpoints endpoints = client.endpoints().inNamespace(namespace).withName(name).get();
        List<String> pods = new ArrayList<>();
        if (endpoints != null) {
            for (EndpointSubset subset : endpoints.getSubsets()) {
                subset.getAddresses().stream()
                    .filter(address -> address.getTargetRef() != null && "Pod".equals(address.getTargetRef().getKind()))
                    .forEach(address -> {
                        String pod = address.getTargetRef().getName();
                        if (pod != null && !pod.isEmpty()) {
                            pods.add(pod);
                        }
                    });
            }
        }
        if (pods.isEmpty()) {
            return null;
        } else {
            String chosen = pods.get(new Random().nextInt(pods.size()));
            return client.pods().inNamespace(namespace).withName(chosen).get();
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
     * Awaits at most 5 minutes until all pods of the application are running.
     */
    public void awaitApplicationReadinessOrFail() {
        await().atMost(5, TimeUnit.MINUTES).until(() -> {
                return client
                    .replicationControllers()
                    .inNamespace(this.namespace)
                    .withName(this.applicationName).isReady();
            }
        );
    }

    public String project() {
        return namespace;
    }

    /**
     * Awaits at most 5 minutes until all pods meets the given predicate.
     *
     * @param filter used to wait to detect that a pod is up and running.
     */
    public void awaitPodReadinessOrFail(Predicate<Pod> filter) {
        await().atMost(5, TimeUnit.MINUTES).until(() -> {
                List<Pod> list = client.pods().inNamespace(namespace).list().getItems();
                return list.stream()
                    .filter(filter)
                    .filter(Readiness::isPodReady)
                    .collect(Collectors.toList()).size() >= 1;
            }
        );
    }

    /**
     * Scaling the application to given replicas
     *
     * @param replicas to scale the application
     */
    public void scale(final int replicas) {
        log.info(String.format("Scaling replicas from %s to %s.", getPods("deployment").size(), replicas));
        this.client
            .replicationControllers()
            .inNamespace(this.namespace)
            .withName(this.applicationName)
            .scale(replicas);

        // ideally, we'd look at deployment status.availableReplicas field,
        // but that's only available since OpenShift 3.5
        await().atMost(5, TimeUnit.MINUTES)
            .until(() -> {
                final List<Pod> pods = getPods("deployment");
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

    protected List<Pod> getPods(String label) {
        return this.client
            .pods()
            .inNamespace(this.namespace)
            .withLabel(label, this.applicationName)
            .list()
            .getItems();
    }

    /**
     * Method that returns the current replication controller object
     *
     * @return Current replication controller object.
     */
    public ReplicationController replicationController() {
        return this.client
            .replicationControllers()
            .inNamespace(this.namespace)
            .withName(this.applicationName)
            .get();
    }
}


