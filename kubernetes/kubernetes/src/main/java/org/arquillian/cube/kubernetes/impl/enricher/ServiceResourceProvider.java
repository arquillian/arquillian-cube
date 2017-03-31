package org.arquillian.cube.kubernetes.impl.enricher;

import io.fabric8.kubernetes.api.model.Service;
import java.lang.annotation.Annotation;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

/**
 * A {@link ResourceProvider} for {@link io.fabric8.kubernetes.api.model.ServiceList}.
 * It refers to services that have been created during the current session.
 */
public class ServiceResourceProvider extends AbstractKubernetesResourceProvider {

    @Override
    public boolean canProvide(Class<?> type) {
        return Service.class.isAssignableFrom(type);
    }

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        return getClient().services().inNamespace(getSession().getNamespace()).withName(getName(qualifiers)).get();
    }
}
