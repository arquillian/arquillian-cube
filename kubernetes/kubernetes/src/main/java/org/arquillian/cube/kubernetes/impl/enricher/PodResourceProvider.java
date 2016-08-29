package org.arquillian.cube.kubernetes.impl.enricher;

import io.fabric8.kubernetes.api.model.Pod;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

import java.lang.annotation.Annotation;

/**
 * A {@link ResourceProvider} for {@link io.fabric8.kubernetes.api.model.PodList}.
 * It refers to pods that have been created during the current session.
 */
public class PodResourceProvider extends AbstractKubernetesResourceProvider {

    @Override
    public boolean canProvide(Class<?> type) {
        return Pod.class.isAssignableFrom(type);
    }

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        return getClient().pods().inNamespace(getSession().getNamespace()).withName(getName(qualifiers)).get();
    }
}
