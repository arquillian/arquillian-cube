package org.arquillian.cube.openshift.impl.client;

import io.fabric8.kubernetes.api.model.v3_1.HasMetadata;
import io.fabric8.kubernetes.api.model.v3_1.Pod;
import io.fabric8.kubernetes.clnt.v3_1.internal.readiness.Readiness;
import io.fabric8.openshift.api.model.v3_1.DeploymentConfig;
import io.fabric8.openshift.api.model.v3_1.Project;
import io.fabric8.openshift.api.model.v3_1.Route;
import io.fabric8.openshift.clnt.v3_1.OpenShiftClient;
import org.arquillian.cube.kubernetes.impl.KubernetesAssistant;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.logging.Logger;

import static org.arquillian.cube.openshift.impl.client.OpenShiftRouteLocator.createUrlFromRoute;
import static org.arquillian.cube.openshift.impl.client.ResourceUtil.awaitRoute;
import static org.awaitility.Awaitility.await;

/**
 * Class that allows you to deploy undeploy and wait for resources programmatically in a test.
 *
 */
public class OpenShiftAssistant extends KubernetesAssistant {

    private static final Logger log = Logger.getLogger(OpenShiftAssistant.class.getName());

    private OpenShiftAssistantDefaultResourcesLocator openShiftAssistantDefaultResourcesLocator;

    OpenShiftAssistant(OpenShiftClient client, String namespace) {
        super(client, namespace);
        this.openShiftAssistantDefaultResourcesLocator = new OpenShiftAssistantDefaultResourcesLocator();
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
    @Override
    public void deployApplication(String applicationName) throws IOException {

        final Optional<URL> defaultFileOptional = this.openShiftAssistantDefaultResourcesLocator.locate();

        if (defaultFileOptional.isPresent()) {
            deployApplication(applicationName, defaultFileOptional.get());
        } else {
            log.warning("No default Kubernetes or OpenShift resources found at default locations.");
        }

    }

    /**
     * Deploys application reading resources from specified InputStream.
     *
     * @param inputStream  where resources are read
     * @throws IOException
     */
    @Override
    public void deploy(InputStream inputStream) throws IOException {
        final List<? extends HasMetadata> entities = deploy("application", inputStream);

        if (this.applicationName == null) {

            Optional<String> deploymentConfig = entities.stream()
                .filter(hm -> hm instanceof DeploymentConfig)
                .map(hm -> (DeploymentConfig) hm)
                .map(dc -> dc.getMetadata().getName()).findFirst();

            deploymentConfig.ifPresent(name -> this.applicationName = name);
        }
    }

    /**
     * Gets the URL of the route with given name.
     * @param routeName to return its URL
     * @return URL backed by the route with given name.
     */
    public Optional<URL> getRoute(String routeName) {
        Route route = getClient().routes()
            .inNamespace(namespace).withName(routeName).get();

        return route != null ? Optional.ofNullable(createUrlFromRoute(route)) : Optional.empty();
    }

    /**
     * Returns the URL of the first route.
     * @return URL backed by the first route.
     */
    public Optional<URL> getRoute() {
        Optional<Route> optionalRoute = getClient().routes().inNamespace(namespace)
            .list().getItems()
            .stream()
            .findFirst();

        return optionalRoute
            .map(OpenShiftRouteLocator::createUrlFromRoute);
    }

    /**
     * Waits until the url responds with correct status code
     * @param routeUrl URL to check (usually a route one)
     * @param statusCodes list of status code that might return that service is up and running.
     *                    It is used as OR, so if one returns true, then the route is considered valid.
     *                    If not set, then only 200 status code is used.
     */
    public void awaitUrl(URL routeUrl, int... statusCodes) {
        awaitRoute(routeUrl, statusCodes);
    }

    /**
     * Scaling the application to given replicas
     * @param replicas to scale the application
     */
    @Override
    public void scale(final int replicas) {
        log.info(String.format("Scaling replicas from %s to %s.", getPods("deploymentconfig").size(), replicas));
        getClient()
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

    /**
     * Awaits at most 5 minutes until all pods of the application are running.
     */
    @Override
    public void awaitApplicationReadinessOrFail() {
        await().atMost(5, TimeUnit.MINUTES).until(() -> {
                return getClient()
                    .deploymentConfigs()
                    .inNamespace(this.namespace)
                    .withName(this.applicationName).isReady();
            }
        );
    }

    /**
     * Method that returns the current deployment configuration object
     * @return Current deployment config object.
     */
    public DeploymentConfig deploymentConfig() {
        return getClient()
            .deploymentConfigs()
            .inNamespace(this.namespace)
            .withName(this.applicationName)
            .get();
    }

    /**
     * Gets template URL used for deploying application.
     *
     * @param templateURL url path to the template
     * @return OpenShiftAssistantTemplate object.
     */
    public OpenShiftAssistantTemplate usingTemplate(URL templateURL) {
        return new OpenShiftAssistantTemplate(templateURL, getClient());
    }

    /**
     * Gets template URL string used for deploying application.
     *
     * @param templateURL path to the template
     * @return OpenShiftAssistantTemplate object.
     */
    public OpenShiftAssistantTemplate usingTemplate(String templateURL) throws MalformedURLException {
        return new OpenShiftAssistantTemplate(new URL(templateURL), getClient());
    }

    /**
     * Gets the list of all the OpenShift Projects.
     *
     * @return list of OpenShift Projects.
     */
    public List<Project> listProjects() {
        return getClient().projects().list().getItems();
    }

    /**
     * Gets the current OpenShift project used for deploying application.
     *
     * @return current namespace.
     */
    public String getCurrentProjectName() {
        return getClient().getNamespace();
    }

    /**
     * Checks if the given project exists or not.
     *
     * @param name project name
     * @return true/false
     * @throws IllegalArgumentException
     */
    public boolean projectExists(String name) throws IllegalArgumentException {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Project name cannot be empty");
        }
        return listProjects().stream()
            .map(p -> p.getMetadata().getName())
            .anyMatch(Predicate.isEqual(name));
    }

    /**
     * Finds for the given project.
     *
     * @param name project name
     * @return given project or an empty {@code Optional} if project does not exist
     * @throws IllegalArgumentException
     */
    public Optional<Project> findProject(String name) throws IllegalArgumentException {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Project name cannot be empty");
        }
        return getProject(name);
    }

    private Optional<Project> getProject(String name) {
        return listProjects().stream()
            .filter(p -> p.getMetadata().getName().equals(name))
            .findFirst();
    }

    public OpenShiftClient getClient() {
        return client.adapt(OpenShiftClient.class);
    }
}
