package org.arquillian.cube.kubernetes.impl.enricher.internal;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.v3_1.Service;
import io.fabric8.kubernetes.api.model.v3_1.ServiceList;

import org.arquillian.cube.kubernetes.impl.enricher.AbstractKubernetesResourceProvider;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

/**
 * A {@link ResourceProvider} for {@link io.fabric8.kubernetes.api.model.v3_1.ServiceList}.
 * It refers to services that have been created during the current session.
 */
public class ServiceResourceProvider extends AbstractKubernetesResourceProvider {

    @Override
    public boolean canProvide(Class<?> type) {
        return Service.class.isAssignableFrom(type);
    }

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        String name = getName(qualifiers);
        String namespace = getNamespace(qualifiers);
        if (name != null) {
            return getClient().services().inNamespace(namespace).withName(name).get();
        }

        // Gets the first service found that matches the labels.
        Map<String, String> labels = getLabels(qualifiers);
        ServiceList list = getClient().services().inNamespace(namespace).withLabels(labels).list();
        List<Service> services = list.getItems();
        if( !services.isEmpty() ) {
            return services.get(0);
        }

        return null;
    }
}
