package org.arquillian.cube.kubernetes.impl.enricher.external;

import io.fabric8.kubernetes.api.model.v2_2.Service;
import java.lang.annotation.Annotation;

import org.arquillian.cube.kubernetes.impl.enricher.AbstractKubernetesResourceProvider;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

/**
 * A {@link ResourceProvider} for {@link io.fabric8.kubernetes.api.model.v2_2.ServiceList}.
 * It refers to services that have been created during the current session.
 */
public class ServiceResourceProvider extends AbstractKubernetesResourceProvider {

    @Override
    public boolean canProvide(Class<?> type) {
        return internalToUserType(Service.class.getName()).equals(type.getName());
    }

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        return toUsersResource(getClient().services().inNamespace(getSession().getNamespace()).withName(getName(qualifiers)).get());
    }
}
