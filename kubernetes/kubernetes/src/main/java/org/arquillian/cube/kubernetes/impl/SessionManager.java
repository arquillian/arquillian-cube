package org.arquillian.cube.kubernetes.impl;

import static org.arquillian.cube.impl.util.SystemEnvironmentVariables.propertyToEnvironmentVariableName;
import static org.arquillian.cube.kubernetes.impl.utils.ProcessUtil.runCommand;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.arquillian.cube.impl.util.Strings;
import org.arquillian.cube.kubernetes.api.AnnotationProvider;
import org.arquillian.cube.kubernetes.api.Configuration;
import org.arquillian.cube.kubernetes.api.DependencyResolver;
import org.arquillian.cube.kubernetes.api.FeedbackProvider;
import org.arquillian.cube.kubernetes.api.KubernetesResourceLocator;
import org.arquillian.cube.kubernetes.api.Logger;
import org.arquillian.cube.kubernetes.api.NamespaceService;
import org.arquillian.cube.kubernetes.api.ResourceInstaller;
import org.arquillian.cube.kubernetes.api.Session;
import org.arquillian.cube.kubernetes.api.SessionCreatedListener;
import org.jboss.arquillian.core.spi.Validate;
import org.xnio.IoUtils;

import io.fabric8.kubernetes.api.model.v2_6.Container;
import io.fabric8.kubernetes.api.model.v2_6.Endpoints;
import io.fabric8.kubernetes.api.model.v2_6.Event;
import io.fabric8.kubernetes.api.model.v2_6.HasMetadata;
import io.fabric8.kubernetes.api.model.v2_6.Pod;
import io.fabric8.kubernetes.api.model.v2_6.PodList;
import io.fabric8.kubernetes.api.model.v2_6.ReplicationController;
import io.fabric8.kubernetes.api.model.v2_6.ReplicationControllerList;
import io.fabric8.kubernetes.api.model.v2_6.Service;
import io.fabric8.kubernetes.api.model.v2_6.ServiceList;
import io.fabric8.kubernetes.api.model.v2_6.ServicePort;
import io.fabric8.kubernetes.api.model.v2_6.extensions.ReplicaSet;
import io.fabric8.kubernetes.api.model.v2_6.extensions.ReplicaSetList;
import io.fabric8.kubernetes.clnt.v2_6.KubernetesClient;
import io.fabric8.kubernetes.clnt.v2_6.KubernetesClientException;
import io.fabric8.kubernetes.clnt.v2_6.KubernetesClientTimeoutException;
import io.fabric8.kubernetes.clnt.v2_6.Watch;
import io.fabric8.kubernetes.clnt.v2_6.Watcher;
import io.fabric8.kubernetes.clnt.v2_6.dsl.LogWatch;

public class SessionManager implements SessionCreatedListener {

    private final Session session;
    private final KubernetesClient client;
    private final Configuration configuration;
    private final AnnotationProvider annotationProvider;
    private final NamespaceService namespaceService;
    private final KubernetesResourceLocator kubernetesResourceLocator;
    private final DependencyResolver dependencyResolver;
    private final ResourceInstaller resourceInstaller;
    private final FeedbackProvider feedbackProvider;

    private final List<HasMetadata> resources = new ArrayList<>();
    private final Map<String, Collection<Closeable>> watchersMap = new HashMap<String, Collection<Closeable>>();
    private Watch watchLog;
    private Watch watchEvents;

    private final AtomicReference<ShutdownHook> shutdownHookRef = new AtomicReference<>();

    private String logPath;
    private FileWriter eventLogWriter;
    private String currentClassName;
    private String currentMethodName;

    public SessionManager(Session session, KubernetesClient client, Configuration configuration,
        AnnotationProvider annotationProvider, NamespaceService namespaceService,
        KubernetesResourceLocator kubernetesResourceLocator,
        DependencyResolver dependencyResolver, ResourceInstaller resourceInstaller, FeedbackProvider feedbackProvider) {

        Validate.notNull(session, "A Session instance is required.");
        Validate.notNull(client, "A KubernetesClient instance is required.");
        Validate.notNull(configuration, "Configuration is required.");
        Validate.notNull(annotationProvider, "An AnnotationProvider instance is required.");
        Validate.notNull(namespaceService, "A NamespaceService instance is required.");
        Validate.notNull(dependencyResolver, "A DependencyResolver instance is required.");
        Validate.notNull(kubernetesResourceLocator, "A KubernetesResourceLocator instance is required.");
        Validate.notNull(resourceInstaller, "A ResourceInstaller instance is required.");
        Validate.notNull(feedbackProvider, "A FeedbackProvider instance is required.");
        this.session = session;
        this.client = client;
        this.configuration = configuration;
        this.annotationProvider = annotationProvider;
        this.namespaceService = namespaceService;
        this.kubernetesResourceLocator = kubernetesResourceLocator;
        this.dependencyResolver = dependencyResolver;
        this.resourceInstaller = resourceInstaller;
        this.feedbackProvider = feedbackProvider;
    }

    private String getSessionStatus() {
        if (session.getFailed().get() > 0) {
            return "FAILED";
        } else {
            return "PASSED";
        }
    }

    /**
     * Creates a namespace if needed.
     */
    public void createNamespace() {
        Map<String, String> namespaceAnnotations = annotationProvider.create(session.getId(), Constants.RUNNING_STATUS);
        if (namespaceService.exists(session.getNamespace())) {
            //namespace exists
        } else if (configuration.isNamespaceLazyCreateEnabled()) {
            namespaceService.create(session.getNamespace(), namespaceAnnotations);
        } else {
            throw new IllegalStateException("Namespace [" + session.getNamespace() + "] doesn't exist and lazily creation of namespaces is disabled. "
            + "Either use an existing one, or set `namespace.lazy.enabled` to true.");
        }
    }


    public void createEnvironment() {
        Logger log = session.getLogger();
        try {
            URL configUrl = configuration.getEnvironmentConfigUrl();
            List<URL> dependencyUrls =
                !configuration.getEnvironmentDependencies().isEmpty() ? configuration.getEnvironmentDependencies()
                    : dependencyResolver.resolve(session);

            if (configuration.isEnvironmentInitEnabled()) {

                if (configuration.getEnvironmentSetupScriptUrl() != null) {
                    setupEnvironment();
                }

                Collection<URL> additionalUrls = kubernetesResourceLocator.locateAdditionalResources();
                for (URL url : additionalUrls) {
                    log.status("Applying additional kubernetes configuration from: " + url);
                    try (InputStream is = url.openStream()) {
                        resources.addAll(resourceInstaller.install(url));
                    }
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

                List<HasMetadata> resourcesToWait = new ArrayList<>(resources);

                //Also handle services externally specified
                for (String service : configuration.getWaitForServiceList()) {
                    Endpoints endpoints = client.endpoints().inNamespace(session.getNamespace()).withName(service).get();
                    if (endpoints != null) {
                        resourcesToWait.add(endpoints);
                    }
                }

                if (!resourcesToWait.isEmpty()) {
                    try {
                        client.resourceList(resourcesToWait)
                            .waitUntilReady(configuration.getWaitTimeout(), TimeUnit.MILLISECONDS);
                    } catch (KubernetesClientTimeoutException t) {
                        log.warn("There are resources in not ready state:");
                        for (HasMetadata r : t.getResourcesNotReady()) {
                            log.error(
                                r.getKind() + " name: " + r.getMetadata().getName() + " namespace:" + r.getMetadata()
                                    .getNamespace());
                            feedbackProvider.onResourceNotReady(r);
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
            }
            throw new RuntimeException(e);
        }
    }

    @Override
    public void start() {
        Logger log = session.getLogger();
        log.status("Using Kubernetes at: " + client.getMasterUrl());
        createNamespace();
        setupConsoleListener();
        setupEventListener();

        addShutdownHook();
        try {
            createEnvironment();
        } catch (Throwable t){
          removeShutdownHook();
          throw t;
        }
    }

    private void addConsole(final String podName) {
        if (watchersMap.containsKey(podName))
            return;

        String className = session.getCurrentClassName();
        String methodName = session.getCurrentMethodName();
        String fileName = logPath;

        if (Strings.isNullOrEmpty(className))
            className = "NOCLASS";
        fileName += String.format("/%s", className);

        if (Strings.isNotNullOrEmpty(methodName))
            fileName += String.format("-%s", methodName);

        try {
            Collection<Closeable> fds = new ArrayList<Closeable>();
            List<Container> containers = client.pods().inNamespace(session.getNamespace()).withName(podName).get()
                    .getSpec().getContainers();
            if (containers.size() == 1) {
                fileName += String.format("-%s.log", podName);
                final FileOutputStream stream = new FileOutputStream(fileName);
                LogWatch lw = client.pods().inNamespace(session.getNamespace()).withName(podName).watchLog(stream);
                fds.add(lw);
                fds.add(stream);
            } else {
                for (Container container : containers) {
                    String containerName = container.getName();
                    String fileNameContainer = String.format("%s-%s-%s.log", fileName, podName, containerName);
                    final FileOutputStream stream = new FileOutputStream(fileNameContainer);
                    LogWatch lw = client.pods().inNamespace(session.getNamespace()).withName(podName).inContainer(containerName).watchLog(stream);
                    fds.add(lw);
                    fds.add(stream);
                }
            }

            watchersMap.put(podName, fds);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(String.format("Error storing the console log for pod %s", podName), e);
        }

    }

    private void delConsole(String podName) {
        Collection<Closeable> lw = watchersMap.get(podName);
        if (lw == null)
            return;

        watchersMap.remove(podName);
        IoUtils.safeClose(lw.toArray(new Closeable[0]));
    }

    private void setupConsoleListener() {
        if (!configuration.isLogCopyEnabled())
            return;

        logPath = configuration.getLogPath();
        if (Strings.isNullOrEmpty(logPath))
            logPath = String.format("%s/target/surefire-reports", System.getProperty("user.dir"));
        session.getLogger().info(String.format("Storing pods console logs into dir %s", logPath));
        new File(logPath).mkdirs();

        final Watcher<Pod> watcher = new Watcher<Pod>() {
            @Override
            public void eventReceived(Action action, Pod pod) {
                switch (action) {
                    case ADDED:
                    case MODIFIED:
                        if (pod.getStatus().getPhase().equalsIgnoreCase("Running")) {
                            addConsole(pod.getMetadata().getName());
                        }
                        break;
                    case DELETED:
                    case ERROR:
                        delConsole(pod.getMetadata().getName());
                        break;
                }
            }

            @Override
            public void onClose(KubernetesClientException cause) {
            }
        };

        watchLog = client.pods().inNamespace(session.getNamespace()).watch(watcher);
    }

    private void cleanupConsoleListener() {
        if (watchLog != null) {
            watchLog.close();
        }
        watchersMap.forEach((k, v) -> {
            IoUtils.safeClose(v.toArray(new Closeable[0]));
        });
        watchersMap.clear();
    }

    private void setupEventLogWriter() {
        String className = session.getCurrentClassName();
        String methodName = session.getCurrentMethodName();

        if (className != null && className.equals(currentClassName)
                && methodName != null && methodName.equals(currentMethodName))
            return;

        currentClassName = className;
        currentMethodName = methodName;
        String fileName = logPath;

        if (Strings.isNullOrEmpty(className))
            className = "NOCLASS";
        fileName += String.format("/%s", className);

        if (Strings.isNotNullOrEmpty(methodName))
            fileName += String.format("-%s", methodName);
        fileName += "-KUBE_EVENTS.log";

        try {
            if (eventLogWriter != null) {
                eventLogWriter.close();
            }
            eventLogWriter = new FileWriter(fileName, true);
        } catch (IOException e) {
            throw new RuntimeException("Error storing kubernetes events", e);
        }

    }

    private void setupEventListener() {
        if (!configuration.isLogCopyEnabled())
            return;


        final Watcher<Event> watcher = new Watcher<Event>() {
            @Override
            public void eventReceived(Action action, Event event) {
                switch (action) {
                    case ADDED:
                    case MODIFIED:
                    case DELETED:
                    case ERROR:
                    try {
                        setupEventLogWriter();
                        eventLogWriter.append(String.format("[%s] [%s] [%s:%s]: (%s) %s\n",
                                event.getLastTimestamp(), event.getType(),
                                event.getInvolvedObject().getKind(), event.getInvolvedObject().getName(),
                                event.getReason(), event.getMessage()));
                        eventLogWriter.flush();
                    } catch (IOException e) {
                        throw new RuntimeException("Error storing kubernetes events", e);
                    }
                }
            }

            @Override
            public void onClose(KubernetesClientException cause) {
            }
        };

        watchEvents = client.events().inNamespace(session.getNamespace()).watch(watcher);
    }

    private void cleanupEventsListener() {
        if (watchEvents != null) {
            watchEvents.close();
        }

        if (eventLogWriter != null) {
            try {
                eventLogWriter.close();
            } catch (IOException e) {
                session.getLogger().error("Error closing kubernetes events file: " + e);
            }
        }
    }

    @Override
    public void stop() {
        try {
            cleanupConsoleListener();
            cleanupEventsListener();
            clean(getSessionStatus());
        } finally {
           removeShutdownHook();
        }
    }

    @Override
    public void clean(String status) {
        String namespace = session.getNamespace();
        try {
            if (configuration.isNamespaceCleanupEnabled()) {
                resourceInstaller.uninstall(resources);
            }

            /*
             * While it does make perfect sense to either clean or destroy,
             * in some cases clean is implicit-ly defined. That can implict-ly disable namespace destruction.
             * So, its more clean if we check of both conditions (double if vs if/else).
             *
             */
            if (configuration.isNamespaceDestroyEnabled()) {
                namespaceService.destroy(namespace);
            } else {
                try {
                    namespaceService.annotate(session.getNamespace(), annotationProvider.create(session.getId(), status));
                } catch (Throwable t) {
                    session.getLogger()
                        .warn("Could not annotate namespace: [" + namespace + "] with status: [" + status + "].");
                }
            }
        } finally {
            tearDownEnvironment();
        }
    }

    @Override
    public void display() {
        ReplicaSetList replicaSetList = client.extensions().replicaSets().inNamespace(session.getNamespace()).list();
        if (replicaSetList.getItems() != null) {
            for (ReplicaSet replicaSet : replicaSetList.getItems()) {
                session.getLogger().info("ReplicaSet: [" + replicaSet.getMetadata().getName() + "]");
            }
        }

        ReplicationControllerList replicationControllerList =
            client.replicationControllers().inNamespace(session.getNamespace()).list();
        if (replicationControllerList.getItems() != null) {
            for (ReplicationController replicationController : replicationControllerList.getItems()) {
                session.getLogger()
                    .info("Replication controller: [" + replicationController.getMetadata().getName() + "]");
            }
        }

        PodList podList = client.pods().inNamespace(session.getNamespace()).list();
        if (podList != null) {
            for (Pod pod : podList.getItems()) {
                session.getLogger()
                    .info("Pod: [" + pod.getMetadata().getName() + "] Status: [" + pod.getStatus().getPhase() + "]");
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

    private void setupEnvironment() {
        Logger log = session.getLogger();
        log.info("Executing environment setup script from:" + configuration.getEnvironmentSetupScriptUrl());
        try {
            runCommand(log, configuration.getEnvironmentSetupScriptUrl(), createScriptEnvironment());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void tearDownEnvironment() {
        if (configuration.getEnvironmentTeardownScriptUrl() != null) {
            try {
                session.getLogger()
                    .info(
                        "Executing environment teardown script from:" + configuration.getEnvironmentTeardownScriptUrl());
                runCommand(session.getLogger(), configuration.getEnvironmentTeardownScriptUrl(),
                    createScriptEnvironment());
            } catch (IOException ex) {
                session.getLogger().warn("Failed to execute teardown script, due to: " + ex.getMessage());
            }
        }
    }

    /**
     * Creates the environment variables, that will be passed to the shell script (startup, teardown).
     */
    private Map<String, String> createScriptEnvironment() {
        Map<String, String> env = new HashMap<>();
        env.putAll(System.getenv());
        env.putAll(configuration.getScriptEnvironmentVariables());
        env.put(propertyToEnvironmentVariableName(Configuration.KUBERNETES_NAMESPACE), configuration.getNamespace());
        env.put(propertyToEnvironmentVariableName(Configuration.KUBERNETES_DOMAIN), configuration.getKubernetesDomain());
        env.put(propertyToEnvironmentVariableName(Configuration.KUBERNETES_MASTER),
            configuration.getMasterUrl().toString());
        env.put(propertyToEnvironmentVariableName(Configuration.DOCKER_REGISTY), configuration.getDockerRegistry());
        return env;
    }

    private void addShutdownHook() {
        ShutdownHook hook = new ShutdownHook(new Runnable() {
            @Override
            public void run() {
                SessionManager.this.clean(Constants.ABORTED_STATUS);
            }
        });

        Runtime.getRuntime().addShutdownHook(hook);
        shutdownHookRef.set(hook);
    }

    /**
     * Removes the {@link ShutdownHook}.
     */
    private void removeShutdownHook() {
        ShutdownHook hook = shutdownHookRef.get();
        if (hook != null) {
            Runtime.getRuntime().removeShutdownHook(hook);
        }
    }
}

