package org.arquillian.cube.kubernetes.impl.enricher.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import io.fabric8.kubernetes.api.model.v2_6.ServiceList;

import org.arquillian.cube.kubernetes.impl.enricher.AbstractKubernetesResourceProvider;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

/**
 * A {@link ResourceProvider} for {@link ServiceList}.
 * It refers to services that have been created during the current session.
 */
public class ServiceListResourceProvider extends AbstractKubernetesResourceProvider {

    @Override
    public boolean canProvide(Class<?> type) {
        return ServiceList.class.isAssignableFrom(type);
    }

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        Map<String, String> labels = getLabels(qualifiers);
        if (labels.isEmpty()) {
            return getClient().services().inNamespace(getSession().getNamespace()).list();
        } else {
            return getClient().services().inNamespace(getSession().getNamespace()).withLabels(labels).list();
        }
    }
}
