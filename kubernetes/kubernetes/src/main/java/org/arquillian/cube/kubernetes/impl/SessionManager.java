package org.arquillian.cube.kubernetes.impl;

import org.arquillian.cube.kubernetes.api.AnnotationProvider;
import org.arquillian.cube.kubernetes.api.Configuration;
import org.arquillian.cube.kubernetes.api.DependencyResolver;
import org.arquillian.cube.kubernetes.api.KubernetesResourceLocator;
import org.arquillian.cube.kubernetes.api.Logger;
import org.arquillian.cube.kubernetes.api.NamespaceService;
import org.arquillian.cube.kubernetes.api.ResourceInstaller;
import org.arquillian.cube.kubernetes.api.Session;
import org.arquillian.cube.kubernetes.api.SessionCreatedListener;
import org.jboss.arquillian.core.spi.Validate;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerList;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientTimeoutException;

import static org.arquillian.cube.kubernetes.impl.utils.ProcessUtil.runCommand;

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

                if (configuration.getEnvironmentSetupScriptUrl() != null) {
                    setupEnvironment();
                }

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
        try {
            if (configuration.isNamespaceCleanupEnabled()) {
                resourceInstaller.uninstall(resources);
            } else if (configuration.isNamespaceDestroyEnabled()) {
                namespaceService.destroy(namespace);
            } else {
                try {
                    namespaceService.annotate(session.getNamespace(), annotationProvider.create(session.getId(), status));
                } catch (Throwable t) {
                    session.getLogger().warn("Could not annotate namespace: [" + namespace + "] with status: [" + status + "].");
                }
            }
        } finally {
            tearDownEnvironment();
        }
    }

    @Override
    public void display() {
        ReplicationControllerList replicationControllerList = client.replicationControllers().inNamespace(session.getNamespace()).list();
        if (replicationControllerList.getItems() != null) {
            for (ReplicationController replicationController :replicationControllerList.getItems()){
                session.getLogger().info("Replication controller: [" + replicationController.getMetadata().getName() + "]");
            }
        }

        PodList podList = client.pods().inNamespace(session.getNamespace()).list();
        if (podList != null) {
            for (Pod pod : podList.getItems()) {
                session.getLogger().info("Pod: [" + pod.getMetadata().getName() + "] Status: [" + pod.getStatus().getPhase() + "]");
            }
        }

        ServiceList serviceList = client.services().inNamespace(session.getNamespace()).list();
        if (serviceList != null) {
            for (Service service : serviceList.getItems()) {

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
    }

    private void setupEnvironment()  {
        Logger log = session.getLogger();
        log.info("Executing environment setup script from:" + configuration.getEnvironmentSetupScriptUrl());
        try {
            runCommand(log, configuration.getEnvironmentSetupScriptUrl());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void tearDownEnvironment() {
        if (configuration.getEnvironmentTeardownScriptUrl() != null) {
            try {
                session.getLogger().info("Executing environment teardown script from:" + configuration.getEnvironmentTeardownScriptUrl());
                runCommand(session.getLogger(), configuration.getEnvironmentTeardownScriptUrl());
            } catch (IOException ex) {
                session.getLogger().warn("Failed to execute teardown script, due to: " + ex.getMessage());
            }
        }
    }

    private static String getSessionStatus(Session session) {
        if (session.getFailed().get() > 0) {
            return "FAILED";
        } else {
            return "PASSED";
        }
    }

    private static boolean isYaml(String s) {
        return s.endsWith(".yml") || s.endsWith(".yaml");
    }

    private static boolean isJson(String s) {
        return s.endsWith(".json");
    }

    private static boolean isShellScript(String s) {
        return s.endsWith(".sh") || s.endsWith(".bash");
    }

}
