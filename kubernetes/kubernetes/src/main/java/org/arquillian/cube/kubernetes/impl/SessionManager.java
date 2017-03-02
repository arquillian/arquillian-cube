package org.arquillian.cube.kubernetes.impl;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientTimeoutException;
import org.arquillian.cube.kubernetes.api.*;
import org.jboss.arquillian.core.spi.Validate;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class SessionManager implements SessionCreatedListener {

    private final Session session;
    private final KubernetesClient client;
    private final Configuration configuration;
    private final AnnotationProvider annotationProvider;
    private final NamespaceService namespaceService;
    private final KubernetesResourceLocator kubernetesResourceLocator;
    private final DependencyResolver dependencyResolver;
    private final ResourceInstaller resourceInstaller;

    private final List<HasMetadata> resources = new ArrayList<>();

    private final AtomicReference<ShutdownHook> shutdownHookRef = new AtomicReference<>();

    public SessionManager(Session session, KubernetesClient client, Configuration configuration,
                          AnnotationProvider annotationProvider, NamespaceService namespaceService,
                          KubernetesResourceLocator kubernetesResourceLocator,
                          DependencyResolver dependencyResolver, ResourceInstaller resourceInstaller) {

        Validate.notNull(session, "A Session instance is required.");
        Validate.notNull(client, "A KubernetesClient instance is required.");
        Validate.notNull(configuration, "Configuration is required.");
        Validate.notNull(annotationProvider, "An AnnotationProvider instance is required.");
        Validate.notNull(namespaceService, "A NamespaceService instance is required.");
        Validate.notNull(dependencyResolver, "A DependencyResolver instance is required.");
        Validate.notNull(kubernetesResourceLocator, "A KubernetesResourceLocator instance is required.");
        Validate.notNull(resourceInstaller, "A ResourceInstaller instance is required.");
        this.session = session;
        this.client = client;
        this.configuration = configuration;
        this.annotationProvider = annotationProvider;
        this.namespaceService = namespaceService;
        this.kubernetesResourceLocator = kubernetesResourceLocator;
        this.dependencyResolver = dependencyResolver;
        this.resourceInstaller = resourceInstaller;
    }

    @Override
    public void start() {
        ShutdownHook hook = null;
        Logger log = session.getLogger();
        String namespace = session.getNamespace();

        log.status("Using Kubernetes at: " + client.getMasterUrl());
        log.status("Creating kubernetes resources inside namespace: " + namespace);
        log.info("if you use OpenShift then type this switch namespaces:     oc project " + namespace);
        log.info("if you use kubernetes then type this to switch namespaces: kubectl namespace " + namespace);

        Map<String, String> namespaceAnnotations = annotationProvider.create(session.getId(), Constants.RUNNING_STATUS);
        if (namespaceService.exists(session.getNamespace())) {
            //namespace exists
        } else if (configuration.isNamespaceLazyCreateEnabled()) {
            namespaceService.create(session.getNamespace(), namespaceAnnotations);
        } else {
            throw new IllegalStateException("Namespace [" + session.getNamespace() + "] doesn't exists");
        }

        hook = new ShutdownHook(new Runnable() {
            @Override
            public void run() {
                SessionManager.this.clean(Constants.ABORTED_STATUS);
            }
        });

        Runtime.getRuntime().addShutdownHook(hook);
        shutdownHookRef.set(hook);

        try {
            URL configUrl = configuration.getEnvironmentConfigUrl();
            List<URL> dependencyUrls = !configuration.getEnvironmentDependencies().isEmpty() ? configuration.getEnvironmentDependencies() : dependencyResolver.resolve(session);

            if (configuration.isEnvironmentInitEnabled()) {
                for (URL dependencyUrl : dependencyUrls) {
                    log.info("Found dependency: " + dependencyUrl);
                    resources.addAll(resourceInstaller.install(dependencyUrl));
                }

                if (configUrl == null) {
                    configUrl = kubernetesResourceLocator.locate();
                }

                if (configUrl != null) {
                    log.status("Applying kubernetes configuration from: " + configUrl);
                    try (InputStream is = configUrl.openStream()) {
                        resources.addAll(resourceInstaller.install(configUrl));
                    }
                } else {
                    log.warn("Did not find any kubernetes configuration.");
                    final URL definitionsFileURL = configuration.getDefinitionsFileURL();
                    if (definitionsFileURL != null) {
                        log.status("Applying openshift configuration from: " + definitionsFileURL);
                        try (InputStream is = definitionsFileURL.openStream()) {
                            resources.addAll(resourceInstaller.install(definitionsFileURL));
                        }
                    }
                }

                if (!resources.isEmpty()) {
                    try {
                        client.resourceList(resources).waitUntilReady(configuration.getWaitTimeout(), TimeUnit.MILLISECONDS);
                    } catch (KubernetesClientTimeoutException t) {
                        log.warn("The are resources in not ready state.");
                        for (HasMetadata r : t.getResourcesNotReady()) {
                            log.error(r.getKind() + " name: " + r.getMetadata().getName() + " namespace:" + r.getMetadata().getNamespace());
                        }
                        throw new IllegalStateException("Environment not initialized in time.", t);
                    }
                }
            }
            display();
        } catch (Exception e) {
            try {
                clean(Constants.ERROR_STATUS);
            } catch (Exception me) {
                throw new RuntimeException(e);
            } finally {
                if (hook != null) {
                    Runtime.getRuntime().removeShutdownHook(hook);
                }
            }
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        try {
            clean(getSessionStatus(session));
        } finally {
            ShutdownHook hook = shutdownHookRef.get();
            if (hook != null) {
                Runtime.getRuntime().removeShutdownHook(hook);
            }
        }
    }

    @Override
    public void clean(String status) {
        String namespace = session.getNamespace();
        if (configuration.isNamespaceCleanupEnabled()) {
            resourceInstaller.uninstall(resources);
        } else if(configuration.isNamespaceDestroyEnabled()) {
            namespaceService.destroy(namespace);
        } else {
            try {
                namespaceService.annotate(session.getNamespace(), annotationProvider.create(session.getId(), status));
            } catch (Throwable t) {
                session.getLogger().warn("Could not annotate namespace: [" + namespace + "] with status: [" + status + "].");
            }
        }
    }

    @Override
    public void display() {
        for (ReplicationController replicationController : client.replicationControllers().inNamespace(session.getNamespace()).list().getItems()) {
            session.getLogger().info("Replication controller: [" + replicationController.getMetadata().getName()+ "]");
        }

        for (Pod pod : client.pods().inNamespace(session.getNamespace()).list().getItems()) {
            session.getLogger().info("Pod: [" + pod.getMetadata().getName() + "] Status: [" + pod.getStatus().getPhase() +"]");
        }
        for (Service service : client.services().inNamespace(session.getNamespace()).list().getItems()) {

            StringBuilder sb = new StringBuilder();
            sb.append("Service: [").append(service.getMetadata().getName()).append("]")
                    .append(" IP: [").append(service.getSpec().getClusterIP()).append("]")
                    .append(" Ports: [ ");

            for (ServicePort servicePort : service.getSpec().getPorts()) {
                sb.append(servicePort.getPort()).append(" ");
            }
            sb.append("]");
            session.getLogger().info(sb.toString());
        }
    }

    private String getSessionStatus(Session session) {
        if (session.getFailed().get() > 0) {
            return "FAILED";
        } else {
            return "PASSED";
        }
    }

}
