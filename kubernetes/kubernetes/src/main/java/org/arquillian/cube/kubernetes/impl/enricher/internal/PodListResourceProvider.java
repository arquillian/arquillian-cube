package org.arquillian.cube.kubernetes.impl.enricher.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import io.fabric8.kubernetes.api.model.v2_6.PodList;

import org.arquillian.cube.kubernetes.impl.enricher.AbstractKubernetesResourceProvider;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

/**
 * A {@link ResourceProvider} for {@link PodList}.
 * It refers to pods that have been created during the current session.
 */
public class PodListResourceProvider extends AbstractKubernetesResourceProvider {

    @Override
    public boolean canProvide(Class<?> type) {
        return PodList.class.isAssignableFrom(type);
    }

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        Map<String, String> labels = getLabels(qualifiers);
        if( labels.isEmpty() ) {
            return getClient().pods().inNamespace(getSession().getNamespace()).list();
        } else {
            return getClient().pods().inNamespace(getSession().getNamespace()).withLabels(labels).list();
        }
    }
}
