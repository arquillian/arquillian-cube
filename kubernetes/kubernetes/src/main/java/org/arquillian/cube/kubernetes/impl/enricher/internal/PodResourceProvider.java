package org.arquillian.cube.kubernetes.impl.enricher.internal;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.v2_6.Pod;
import io.fabric8.kubernetes.api.model.v2_6.PodList;

import org.arquillian.cube.kubernetes.impl.enricher.AbstractKubernetesResourceProvider;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

/**
 * A {@link ResourceProvider} for {@link io.fabric8.kubernetes.api.model.v2_6.PodList}.
 * It refers to pods that have been created during the current session.
 */
public class PodResourceProvider extends AbstractKubernetesResourceProvider {

    @Override
    public boolean canProvide(Class<?> type) {
        return Pod.class.isAssignableFrom(type);
    }

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        String name = getName(qualifiers);
        if (name != null) {
            return getClient().pods().inNamespace(getSession().getNamespace()).withName(name).get();
        }

        // Gets the first pod found that matches the labels.
        Map<String, String> labels = getLabels(qualifiers);
        PodList list = getClient().pods().inNamespace(getSession().getNamespace()).withLabels(labels).list();
        List<Pod> pods = list.getItems();
        if( !pods.isEmpty() ) {
            return pods.get(0);
        }

        return null;
    }
}
