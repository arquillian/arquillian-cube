package org.arquillian.cube.kubernetes.impl.enricher.external;

import java.lang.annotation.Annotation;

import io.fabric8.kubernetes.api.model.v2_6.Service;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

/**
 * A {@link ResourceProvider} for {@link io.fabric8.kubernetes.api.model.v2_6.ServiceList}.
 * It refers to services that have been created during the current session.
 */
public class ServiceResourceProvider extends org.arquillian.cube.kubernetes.impl.enricher.internal.ServiceResourceProvider {

    @Override
    public boolean canProvide(Class<?> type) {
        return internalToUserType(Service.class.getName()).equals(type.getName());
    }

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        return toUsersResource(super.lookup(resource, qualifiers));
    }
}
