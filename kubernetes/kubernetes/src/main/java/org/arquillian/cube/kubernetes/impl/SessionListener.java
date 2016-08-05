package org.arquillian.cube.kubernetes.impl;

import io.fabric8.kubernetes.api.builder.Visitor;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.arquillian.cube.impl.util.Strings;
import org.arquillian.cube.kubernetes.api.AnnotationProvider;
import org.arquillian.cube.kubernetes.api.Configuration;
import org.arquillian.cube.kubernetes.api.DependencyResolver;
import org.arquillian.cube.kubernetes.api.KubernetesResourceLocator;
import org.arquillian.cube.kubernetes.api.Logger;
import org.arquillian.cube.kubernetes.api.NamespaceService;
import org.arquillian.cube.kubernetes.api.Session;
import org.arquillian.cube.kubernetes.impl.await.WaitStrategy;
import org.arquillian.cube.kubernetes.impl.event.Start;
import org.arquillian.cube.kubernetes.impl.event.Stop;
import org.arquillian.cube.kubernetes.impl.visitor.CompositeVisitor;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.ServiceLoader;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class SessionListener {

    @Inject
    Instance<KubernetesClient> kubernetesClient;

    @Inject
    Instance<Configuration> configuration;

    @Inject
    Instance<AnnotationProvider> annotationProvider;

    @Inject
    Instance<NamespaceService> namespaceService;

    @Inject
    Instance<KubernetesResourceLocator> kubernetesResourceLocator;

    @Inject
    Instance<DependencyResolver> dependencyResolver;

    @Inject
    Instance<ServiceLoader> serviceLoader;

    private ShutdownHook shutdownHook;

    public void start(final @Observes Start event) throws Exception {
        final KubernetesClient kubernetesClient = this.kubernetesClient.get();
        final ServiceLoader serviceLoader = this.serviceLoader.get();
        final Configuration configuration = this.configuration.get();
        final NamespaceService namespaceService = this.namespaceService.get();
        final Session session = event.getSession();
        Logger log = session.getLogger();
        String namespace = session.getNamespace();

        log.status("Using Kubernetes at: " + kubernetesClient.getMasterUrl());
        log.status("Creating kubernetes resources inside namespace: " + namespace);
        log.info("if you use OpenShift then type this switch namespaces:     oc project " + namespace);
        log.info("if you use kubernetes then type this to switch namespaces: kubectl namespace " + namespace);


        String namespaceToUse = configuration.getNamespace();
        if (Strings.isNullOrEmpty(namespaceToUse)) {
            namespaceService.create(session.getNamespace());
        } else if (namespaceService.exists(session.getNamespace())) {
            //namespace exists
        } else if (configuration.isNamespaceLazyCreateEnabled()) {
            namespaceService.create(session.getNamespace());
        } else {
            throw new IllegalStateException("Namespace [" + session.getNamespace() + "] doesn't exists");
        }

        shutdownHook = new ShutdownHook(new Runnable() {
            @Override
            public void run() {
                SessionListener.this.cleanupSession(session, Constants.ABORTED_STATUS);
            }
        });

        Runtime.getRuntime().addShutdownHook(shutdownHook);


        WaitStrategy waitStrategy = new WaitStrategy(kubernetesClient, session, configuration);
        List<Visitor> visitors = new ArrayList<>(serviceLoader.all(Visitor.class));
        visitors.add(waitStrategy);
        CompositeVisitor compositeVisitor = new CompositeVisitor(visitors);

        try {
            URL configUrl = configuration.getEnvironmentConfigUrl();
            List<URL> dependencyUrls = !configuration.getEnvironmentDependencies().isEmpty() ? configuration.getEnvironmentDependencies() : dependencyResolver.get().resolve(session);

            if (configuration.isEnvironmentInitEnabled()) {
                for (URL dependencyUrl : dependencyUrls) {
                    log.info("Found dependency: " + dependencyUrl);
                    try (InputStream is = dependencyUrl.openStream()) {
                        kubernetesClient.load(is).accept(compositeVisitor).apply();
                    }
                }

                if (configUrl == null) {
                    configUrl = kubernetesResourceLocator.get().locate();
                }

                if (configUrl != null) {
                    log.status("Applying kubernetes configuration from: " + configUrl);
                    try (InputStream is = configUrl.openStream()) {
                        kubernetesClient.load(is).accept(compositeVisitor).apply();
                    }
                } else {
                    log.warn("Did not find any kubernetes configuration.");
                }
            }

            if (!configuration.isEnvironmentInitEnabled() || waitStrategy.await()) {
                displaySessionStatus(session);
            } else {
                throw new IllegalStateException("Environment not initialized in time.");
            }
        } catch (Exception e) {
            try {
                cleanupSession(session, Constants.ERROR_STATUS);
            } catch (Exception me) {
                throw e;
            } finally {
                if (shutdownHook != null) {
                    Runtime.getRuntime().removeShutdownHook(shutdownHook);
                }
            }
            throw new RuntimeException(e);
        }
    }

    public void stop(@Observes Stop event, Configuration configuration) throws Exception {
        try {
            Session session = event.getSession();
            cleanupSession(session, getSessionStatus(session));
        } finally {
            if (shutdownHook != null) {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            }
        }
    }


    private void cleanupSession(Session session, String status)  {
        NamespaceService namespaceService = this.namespaceService.get();
        AnnotationProvider annotationProvider = this.annotationProvider.get();
        Configuration configuration = this.configuration.get();
        String namespace = session.getNamespace();

        if (configuration.isNamespaceCleanupEnabled()) {
            namespaceService.clean(namespace);
            namespaceService.destroy(namespace);
        } else {
            namespaceService.annotate(session.getNamespace(), annotationProvider.create(session.getId(), status));
        }
    }

    private void displaySessionStatus(Session session) throws Exception {
        KubernetesClient kubernetesClient = this.kubernetesClient.get();
        for (ReplicationController replicationController : kubernetesClient.replicationControllers().inNamespace(session.getNamespace()).list().getItems()) {
            session.getLogger().info("Replication controller: [" + replicationController.getMetadata().getName()+ "]");
        }

        for (Pod pod : kubernetesClient.pods().inNamespace(session.getNamespace()).list().getItems()) {
            session.getLogger().info("Pod: [" + pod.getMetadata().getName() + "] Status: [" + pod.getStatus().getPhase() +"]");
        }
        for (Service service : kubernetesClient.services().inNamespace(session.getNamespace()).list().getItems()) {

            StringBuilder sb = new StringBuilder();
            sb.append("Service: [").append(service.getMetadata().getName()).append("]")
                    .append(" IP: [").append(service.getSpec().getPortalIP()).append("]")
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
