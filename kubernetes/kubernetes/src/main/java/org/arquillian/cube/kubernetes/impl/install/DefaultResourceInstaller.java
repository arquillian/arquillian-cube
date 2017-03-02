package org.arquillian.cube.kubernetes.impl.install;

import org.arquillian.cube.kubernetes.api.Configuration;
import org.arquillian.cube.kubernetes.api.Logger;
import org.arquillian.cube.kubernetes.api.ResourceInstaller;
import org.arquillian.cube.kubernetes.impl.visitor.CompositeVisitor;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.spi.ServiceLoader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.builder.Visitor;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;

public class DefaultResourceInstaller implements ResourceInstaller {

    @Inject
    protected Instance<KubernetesClient> client;

    @Inject
    protected Instance<Configuration> configuration;

    @Inject
    protected Instance<Logger> logger;

    @Inject
    protected Instance<ServiceLoader> serviceLoader;

    @Override
    public List<HasMetadata> install(URL url) {
        ServiceLoader serviceLoader = this.serviceLoader.get();
        KubernetesClient client = this.client.get();
        List<Visitor> visitors = new ArrayList<>(serviceLoader.all(Visitor.class));
        CompositeVisitor compositeVisitor = new CompositeVisitor(visitors);
        try (InputStream is = url.openStream()) {
            return client.load(is).accept(compositeVisitor).createOrReplace();
        } catch (Throwable t) {
            throw KubernetesClientException.launderThrowable(t);
        }
    }

    @Override
    public Map<HasMetadata, Boolean> uninstall(URL url) {
        ServiceLoader serviceLoader = this.serviceLoader.get();
        KubernetesClient client = this.client.get();

        Map<HasMetadata, Boolean> result = new HashMap<>();
        List<Visitor> visitors = new ArrayList<>(serviceLoader.all(Visitor.class));
        CompositeVisitor compositeVisitor = new CompositeVisitor(visitors);

        try (InputStream is = url.openStream()) {
            return uninstall(client.load(is).accept(compositeVisitor).get());
        } catch (Throwable t) {
            throw KubernetesClientException.launderThrowable(t);
        }
    }

    @Override
    public Map<HasMetadata, Boolean> uninstall(List<HasMetadata> list) {
        KubernetesClient client = this.client.get();
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
        Logger logger = this.logger.get();
        Configuration configuration = this.configuration.get();
        if (configuration.isNamespaceCleanupEnabled()) {
            logger.info("");
            logger.info("Waiting to cleanup the namespace.");
            logger.info("Please type: [Q] to cleanup the namespace.");

            while (true) {
                try {
                    int ch = System.in.read();
                    if (ch < 0 || ch == 'Q') {
                        logger.info("Cleaning up...");
                        break;
                    } else {
                        logger.info("Found character: " + Character.toString((char) ch));
                    }
                } catch (IOException e) {
                    logger.warn("Failed to read from input. " + e);
                    break;
                }
            }
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
}
