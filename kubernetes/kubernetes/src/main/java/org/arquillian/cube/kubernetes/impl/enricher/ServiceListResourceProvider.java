package org.arquillian.cube.kubernetes.impl.enricher;

import io.fabric8.kubernetes.api.model.ServiceList;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

import java.lang.annotation.Annotation;

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
        return getClient().services().inNamespace(getSession().getNamespace()).list();
    }
}
