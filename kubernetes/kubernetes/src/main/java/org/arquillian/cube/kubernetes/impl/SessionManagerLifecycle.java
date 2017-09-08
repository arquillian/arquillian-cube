package org.arquillian.cube.kubernetes.impl;

import io.fabric8.kubernetes.clnt.v2_6.KubernetesClient;
import java.util.concurrent.atomic.AtomicReference;
import org.arquillian.cube.kubernetes.api.AnnotationProvider;
import org.arquillian.cube.kubernetes.api.Configuration;
import org.arquillian.cube.kubernetes.api.DependencyResolver;
import org.arquillian.cube.kubernetes.api.FeedbackProvider;
import org.arquillian.cube.kubernetes.api.KubernetesResourceLocator;
import org.arquillian.cube.kubernetes.api.NamespaceService;
import org.arquillian.cube.kubernetes.api.ResourceInstaller;
import org.arquillian.cube.kubernetes.api.Session;
import org.arquillian.cube.kubernetes.impl.event.AfterStart;
import org.arquillian.cube.kubernetes.impl.event.Start;
import org.arquillian.cube.kubernetes.impl.event.Stop;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.TestClass;

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
    Instance<ResourceInstaller> resourceInstaller;

    @Inject
    Instance<FeedbackProvider> feedbackProvider;

    @Inject
    Event<AfterStart> afterStartEvent;

    AtomicReference<SessionManager> sessionManagerRef = new AtomicReference<>();

    public void start(final @Observes Start event) throws Exception {

        Session session = event.getSession();
        SessionManager sessionManager = new SessionManager(session, kubernetesClient.get(), configuration.get(),
            annotationProvider.get(),
            namespaceService.get().toImmutable(),
            kubernetesResourceLocator.get().toImmutable(),
            dependencyResolver.get().toImmutable(), resourceInstaller.get().toImmutable(),
            feedbackProvider.get().toImmutable());

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
