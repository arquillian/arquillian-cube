package org.arquillian.cube.kubernetes.impl;

import io.fabric8.kubernetes.clnt.v3_1.KubernetesClient;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
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
import org.jboss.arquillian.test.spi.event.suite.After;
import org.jboss.arquillian.test.spi.event.suite.AfterClass;
import org.jboss.arquillian.test.spi.event.suite.Before;
import org.jboss.arquillian.test.spi.event.suite.BeforeClass;

public class SessionManagerLifecycle {

    private static Logger log = Logger.getLogger(SessionManager.class.getName());

    private String namespace;


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

    public void createNamespaceAtClassScope(@Observes BeforeClass beforeClassEvent, Configuration configuration) throws Exception {
        final TestClass testClass = beforeClassEvent.getTestClass();
        log.info(String.format("Creating environment for %s", testClass.getName()));

        if (configuration.isNamespaceClassScopeEnabled()) {
            createUniqueNamespace(configuration, testClass.getJavaClass().getSimpleName());
        }
    }

    public void createNamespaceAtMethodScope(@Observes Before beforeMethodEvent, Configuration configuration) throws Exception {
        final TestClass testClass = beforeMethodEvent.getTestClass();
        final Method testMethod = beforeMethodEvent.getTestMethod();

        log.info(String.format("Creating environment for %s method %s", testClass.getName(), testMethod));

        if (configuration.isNamespaceMethodScopeEnabled()) {
            createUniqueNamespace(configuration, testMethod.getName());
        }

    }

    public void deleteNamespaceAtClassScope(@Observes AfterClass afterClassEvent, Configuration configuration) {
        final TestClass testClass = afterClassEvent.getTestClass();
        log.info(String.format("Deleting environment for %s", testClass.getName()));

        if (configuration.isNamespaceClassScopeEnabled()) {
            namespaceService.get().delete(namespace);
        }
    }

    public void deleteNamespaceAtMethodScope(@Observes After afterMethodEvent, Configuration configuration) {
        final TestClass testClass = afterMethodEvent.getTestClass();
        final Method testMethod = afterMethodEvent.getTestMethod();
        final Class<?> javaClass = testClass.getJavaClass();
        log.info(String.format("Deleting environment for %s method %s", testClass.getName(), testMethod.getName()));

        if (configuration.isNamespaceMethodScopeEnabled()) {
            namespaceService.get().delete(configuration.getNamespace() + "-" + testMethod.hashCode());
        }

    }

    public void stop(@Observes Stop event, Configuration configuration) throws Exception {
        SessionManager sessionManager = sessionManagerRef.get();
        if (sessionManager != null) {
            sessionManager.stop();
        }
    }

    private void createUniqueNamespace(Configuration configuration, String uniqueKey) {
        namespace = configuration.getNamespace() + "-" + uniqueKey.toLowerCase().replaceAll("[^a-z0-9-]", "-");
        if (namespace.chars().count() > 63) {
            namespace = namespace.substring(0, 63);
            log.warning(String.format("Created project: %s for %s", namespace, uniqueKey));
        }
        namespaceService.get().create(namespace);
    }
}
