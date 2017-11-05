package org.arquillian.cube.kubernetes.impl.enricher.external;

import java.lang.annotation.Annotation;

import io.fabric8.kubernetes.api.model.v2_6.ServiceList;

import org.arquillian.cube.kubernetes.impl.enricher.AbstractKubernetesResourceProvider;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

/**
 * A {@link ResourceProvider} for {@link ServiceList}.
 * It refers to services that have been created during the current session.
 */
public class ServiceListResourceProvider extends org.arquillian.cube.kubernetes.impl.enricher.internal.ServiceListResourceProvider {

    @Override
    public boolean canProvide(Class<?> type) {
        return internalToUserType(ServiceList.class.getName()).equals(type.getName());
    }

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        return toUsersResource(super.lookup(resource, qualifiers));
    }
}
