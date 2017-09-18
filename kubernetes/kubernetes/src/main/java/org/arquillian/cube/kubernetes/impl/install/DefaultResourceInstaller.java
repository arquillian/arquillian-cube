package org.arquillian.cube.kubernetes.impl.install;

import io.fabric8.kubernetes.api.builder.v2_6.Visitor;
import io.fabric8.kubernetes.api.model.v2_6.HasMetadata;
import io.fabric8.kubernetes.clnt.v2_6.KubernetesClient;
import io.fabric8.kubernetes.clnt.v2_6.KubernetesClientException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import org.arquillian.cube.kubernetes.api.Configuration;
import org.arquillian.cube.kubernetes.api.Logger;
import org.arquillian.cube.kubernetes.api.ResourceInstaller;
import org.arquillian.cube.kubernetes.api.WithToImmutable;
import org.arquillian.cube.kubernetes.impl.visitor.CompositeVisitor;
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
                delegate = new DefaultResourceInstaller.ImmutableResourceInstaller(client.get(), configuration.get(),
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
    public Map<HasMetadata, Boolean> uninstall(URL url) {
        return toImmutable().uninstall(url);
    }

    @Override
    public Map<HasMetadata, Boolean> uninstall(List<HasMetadata> list) {
        return toImmutable().uninstall(list);
    }

    public static class ImmutableResourceInstaller implements ResourceInstaller, WithToImmutable<ResourceInstaller> {

        private final KubernetesClient client;
        private final Configuration configuration;
        private final Logger logger;
        private final List<Visitor> visitors;

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
        public Map<HasMetadata, Boolean> uninstall(URL url) {
            Map<HasMetadata, Boolean> result = new HashMap<>();
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
                    Boolean deleted = client.resource(h).delete();
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
                return;
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
