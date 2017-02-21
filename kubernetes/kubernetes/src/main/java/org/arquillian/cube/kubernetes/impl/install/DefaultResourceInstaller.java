package org.arquillian.cube.kubernetes.impl.install;

import org.arquillian.cube.kubernetes.api.ResourceInstaller;
import org.arquillian.cube.kubernetes.impl.visitor.CompositeVisitor;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.spi.ServiceLoader;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import io.fabric8.kubernetes.api.builder.Visitor;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;

public class DefaultResourceInstaller implements ResourceInstaller {

    @Inject
    protected Instance<KubernetesClient> client;

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
}
