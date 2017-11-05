package org.arquillian.cube.kubernetes.impl.enricher.external;

import java.lang.annotation.Annotation;

import io.fabric8.kubernetes.api.model.v2_6.Pod;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

/**
 * A {@link ResourceProvider} for {@link io.fabric8.kubernetes.api.model.v2_6.PodList}.
 * It refers to pods that have been created during the current session.
 */
public class PodResourceProvider extends org.arquillian.cube.kubernetes.impl.enricher.internal.PodResourceProvider {

    @Override
    public boolean canProvide(Class<?> type) {
        return internalToUserType(Pod.class.getName()).equals(type.getName());
    }

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        return toUsersResource(super.lookup(resource, qualifiers));
    }
}
