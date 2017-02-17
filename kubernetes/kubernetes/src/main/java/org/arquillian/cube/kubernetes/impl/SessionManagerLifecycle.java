package org.arquillian.cube.kubernetes.impl;

import org.arquillian.cube.kubernetes.api.AnnotationProvider;
import org.arquillian.cube.kubernetes.api.Configuration;
import org.arquillian.cube.kubernetes.api.DependencyResolver;
import org.arquillian.cube.kubernetes.api.KubernetesResourceLocator;
import org.arquillian.cube.kubernetes.api.NamespaceService;
import org.arquillian.cube.kubernetes.api.Session;
import org.arquillian.cube.kubernetes.impl.event.AfterStart;
import org.arquillian.cube.kubernetes.impl.event.Start;
import org.arquillian.cube.kubernetes.impl.event.Stop;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.ServiceLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import io.fabric8.kubernetes.api.builder.Visitor;
import io.fabric8.kubernetes.client.KubernetesClient;

public class SessionManagerLifecycle {

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

    @Inject
    Event<AfterStart> afterStartEvent;

    AtomicReference<SessionManager> sessionManagerRef = new AtomicReference<>();

    public void start(final @Observes Start event) throws Exception {
        List<Visitor> visitors = new ArrayList<>(serviceLoader.get().all(Visitor.class));

        Session session = event.getSession();
        SessionManager sessionManager = new SessionManager(session, kubernetesClient.get(), configuration.get(),
                annotationProvider.get(),
                namespaceService.get().toImmutable(),
                kubernetesResourceLocator.get().toImmutable(),
                dependencyResolver.get().toImmutable(), visitors);

        sessionManagerRef.set(sessionManager);
        sessionManager.start();
        afterStartEvent.fire(new AfterStart(session));
    }

    public void stop(@Observes Stop event, Configuration configuration) throws Exception {
        SessionManager sessionManager = sessionManagerRef.get();
        if (sessionManager != null) {
            sessionManager.stop();
        }
    }
}
