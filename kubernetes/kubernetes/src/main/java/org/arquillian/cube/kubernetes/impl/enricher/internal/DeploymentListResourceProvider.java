package org.arquillian.cube.kubernetes.impl.enricher.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import io.fabric8.kubernetes.api.model.v2_6.extensions.DeploymentList;

import org.arquillian.cube.kubernetes.impl.enricher.AbstractKubernetesResourceProvider;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

/**
 * A {@link ResourceProvider} for {@link DeploymentList}.
 * It refers to deplotments that have been created during the current session.
 */
public class DeploymentListResourceProvider extends AbstractKubernetesResourceProvider {

    @Override
    public boolean canProvide(Class<?> type) {
        return DeploymentList.class.isAssignableFrom(type);
    }

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        Map<String, String> labels = getLabels(qualifiers);
        if (labels.isEmpty()) {
            return getClient().extensions().deployments().inNamespace(getSession().getNamespace()).list();
        } else {
            return getClient().extensions().deployments().inNamespace(getSession().getNamespace()).withLabels(labels).list();
        }
    }
}
