package org.arquillian.cube.kubernetes.impl.enricher;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

import java.lang.annotation.Annotation;

import io.fabric8.kubernetes.api.model.extensions.DeploymentList;

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
        return getClient().extensions().deployments().inNamespace(getSession().getNamespace()).list();
    }
}
