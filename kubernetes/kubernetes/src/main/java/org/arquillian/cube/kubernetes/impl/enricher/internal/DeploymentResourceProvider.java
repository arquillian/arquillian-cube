package org.arquillian.cube.kubernetes.impl.enricher.internal;

import org.arquillian.cube.kubernetes.impl.enricher.AbstractKubernetesResourceProvider;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.v4_10.apps.Deployment;
import io.fabric8.kubernetes.api.model.v4_10.apps.DeploymentList;

/**
 * A {@link ResourceProvider} for {@link Deployment}.
 * It refers to deployments that have been created during the current session.
 */
public class DeploymentResourceProvider extends AbstractKubernetesResourceProvider {
    @Override
    public boolean canProvide(Class<?> type) {
        return Deployment.class.isAssignableFrom(type);
    }

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        String name = getName(qualifiers);
        String namespace = getNamespace(qualifiers);
        if (name != null) {
            return getClient().extensions()
                .deployments()
                .inNamespace(namespace)
                .withName(name)
                .get();
        }

        // Gets the first deployment that matches the labels.
        Map<String, String> labels = getLabels(qualifiers);
        DeploymentList list = getClient().extensions().deployments().inNamespace(namespace).withLabels(labels).list();
        List<Deployment> deployments = list.getItems();
        if( !deployments.isEmpty() ) {
            return deployments.get(0);
        }

        return null;
    }
}
