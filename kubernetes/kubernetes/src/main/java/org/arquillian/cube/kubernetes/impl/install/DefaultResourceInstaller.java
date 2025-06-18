package org.arquillian.cube.kubernetes.impl.install;

import io.fabric8.kubernetes.api.builder.Visitor;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.client.KubernetesClientTimeoutException;
import org.arquillian.cube.kubernetes.api.Configuration;
import org.arquillian.cube.kubernetes.api.Logger;
import org.arquillian.cube.kubernetes.api.ResourceInstaller;
import org.arquillian.cube.kubernetes.api.WithToImmutable;
import org.arquillian.cube.kubernetes.impl.visitor.CompositeVisitor;
import org.awaitility.Awaitility;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.spi.ServiceLoader;

public class DefaultResourceInstaller implements ResourceInstaller {

    @Inject
    protected Instance<KubernetesClient> client;

    @Inject
    protected Instance<Configuration> configuration;

    @Inject
    protected Instance<Logger> logger;

    @Inject
    protected Instance<ServiceLoader> serviceLoader;

    protected ResourceInstaller delegate;

    @Override
    public ResourceInstaller toImmutable() {
        if (delegate != null) {
            return delegate;
        }
        synchronized (this) {
            if (delegate == null) {
                delegate = new ImmutableResourceInstaller(client.get(), configuration.get(),
                    logger.get().toImmutable(),
                    new ArrayList<>(serviceLoader.get().all(Visitor.class)));
            }
        }
        return delegate;
    }

    @Override
    public List<HasMetadata> install(URL url) {
        return toImmutable().install(url);
    }

    @Override
    public List<HasMetadata> install(URL url, List<String> resourcesToWaitFor) {
        return toImmutable().install(url, resourcesToWaitFor);
    }

    @Override
    public Map<HasMetadata, Boolean> uninstall(URL url) {
        return toImmutable().uninstall(url);
    }

    @Override
    public Map<HasMetadata, Boolean> uninstall(List<HasMetadata> list) {
        return toImmutable().uninstall(list);
    }

    public static class ImmutableResourceInstaller implements ResourceInstaller, WithToImmutable<ResourceInstaller> {

        protected final KubernetesClient client;
        protected final Configuration configuration;
        protected final Logger logger;
        protected final List<Visitor> visitors;

        public ImmutableResourceInstaller(KubernetesClient client, Configuration configuration, Logger logger,
            List<Visitor> visitors) {
            this.client = client;
            this.configuration = configuration;
            this.logger = logger;
            this.visitors = visitors;
        }

        @Override
        public List<HasMetadata> install(URL url) {
            CompositeVisitor compositeVisitor = new CompositeVisitor(visitors);
            try (InputStream is = url.openStream()) {
                return client.load(is).accept(compositeVisitor).createOrReplace();
            } catch (Throwable t) {
                throw KubernetesClientException.launderThrowable(t);
            }
        }

        @Override
        public List<HasMetadata> install(URL url, List<String> resourcesToWaitFor) {
            CompositeVisitor compositeVisitor = new CompositeVisitor(visitors);
            try (InputStream is = url.openStream()) {
                final List<HasMetadata> resources = client.load(is).accept(compositeVisitor).createOrReplace();

                // wait for resources that should be ready after the definition is applied
                final List<HasMetadata> dependedOnResourcesToWaitFor = new ArrayList<>();
                for (String dependedOnResource : resourcesToWaitFor) {
                    // wait for creation
                    final AtomicReference<Optional<? extends HasMetadata>> dependedOnResourceToWaitFor = new AtomicReference<>();
                    Awaitility.await().atMost(configuration.getWaitTimeout(), TimeUnit.MILLISECONDS).until(() -> {
                        // look for the cluster pods which name contains the provided resource name
                        dependedOnResourceToWaitFor.set(client.pods().inNamespace(client.getNamespace()).list().getItems().stream()
                            .filter(p ->
                                p.getMetadata().getName().contains(dependedOnResource)).findFirst()
                        );
                        return dependedOnResourceToWaitFor.get().isPresent();
                    });
                    // if retrieved, check it is not something we've added already, and add it to the final list in such case
                    if (dependedOnResourceToWaitFor.get().isPresent()) {
                        final HasMetadata resource = dependedOnResourceToWaitFor.get().get();
                        final boolean notYetAdded = dependedOnResourcesToWaitFor.stream().noneMatch(dr -> dr.getMetadata().getName().equals(resource.getMetadata().getName()));
                        if (notYetAdded) {
                            dependedOnResourcesToWaitFor.add(resource);
                        }
                    }
                }

                // were all the resources we depend on created?
                List<String> missingDependencies = resourcesToWaitFor.stream()
                    .filter(wr -> dependedOnResourcesToWaitFor.stream().noneMatch( dr -> dr.getMetadata().getName().contains(wr)))
                    .collect(Collectors.toList());
                // if not, log a WARN
                if (!missingDependencies.isEmpty()) {
                    logger.warn(String.format(
                        "The resource %s creation - which the environment depends on - timed out, so Cube will not wait for the related dependency to be ready",
                        String.join(",", missingDependencies))
                    );
                }

                // now, let's wait for the depended on resources to be ready
                if (configuration.isWaitEnabled() && !dependedOnResourcesToWaitFor.isEmpty()) {
                    try {
                        client.resourceList(dependedOnResourcesToWaitFor)
                            .waitUntilReady(configuration.getWaitTimeout(), TimeUnit.MILLISECONDS);
                    } catch (KubernetesClientTimeoutException t) {
                        logger.warn("There are resources declared as dependencies which in not ready state:");
                        for (HasMetadata r : t.getResourcesNotReady()) {
                            logger.error(
                                r.getKind() + " name: " + r.getMetadata().getName() + " namespace:" + r.getMetadata()
                                    .getNamespace());
                        }
                        throw new IllegalStateException("Environment not initialized in time.", t);
                    }
                }

                return resources;

            } catch (Throwable t) {
                throw KubernetesClientException.launderThrowable(t);
            }
        }

        @Override
        public Map<HasMetadata, Boolean> uninstall(URL url) {
            CompositeVisitor compositeVisitor = new CompositeVisitor(visitors);

            try (InputStream is = url.openStream()) {
                return uninstall(client.load(is).accept(compositeVisitor).get());
            } catch (Throwable t) {
                throw KubernetesClientException.launderThrowable(t);
            }
        }

        @Override
        public Map<HasMetadata, Boolean> uninstall(List<HasMetadata> list) {
            Map<HasMetadata, Boolean> result = new HashMap<>();
            preUninstallCheck();
            for (HasMetadata h : list) {
                try {
                    Boolean deleted = client.resource(h).delete().stream().allMatch(d -> d.getCauses().isEmpty());
                    result.put(h, deleted);
                } catch (Throwable t) {
                    result.put(h, false);
                }
            }
            return result;
        }

        public void preUninstallCheck() {
            if (configuration.isNamespaceCleanupConfirmationEnabled()) {
                logger.info("");
                logger.info("Waiting to cleanup the namespace.");
                logger.info("Please press <enter> to cleanup the namespace.");

                Scanner scanner = new Scanner(System.in);
                scanner.nextLine();
                logger.info("Cleaning up...");
            } else {
                long timeout = configuration.getNamespaceCleanupTimeout();
                if (timeout > 0L) {
                    logger.info("");
                    logger.info("Waiting for " + timeout + " seconds before cleaning the namespace");
                    try {
                        Thread.sleep(timeout * 1000);
                    } catch (InterruptedException e) {
                        logger.info("Interrupted waiting to cleanup the namespace: " + e);
                    }
                }
            }
        }

        @Override
        public ResourceInstaller toImmutable() {
            return this;
        }
    }
}
