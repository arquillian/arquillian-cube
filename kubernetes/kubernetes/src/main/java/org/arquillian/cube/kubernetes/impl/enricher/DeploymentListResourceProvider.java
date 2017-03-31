package org.arquillian.cube.kubernetes.impl.enricher;

import io.fabric8.kubernetes.api.model.extensions.DeploymentList;
import java.lang.annotation.Annotation;
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
        return getClient().extensions().deployments().inNamespace(getSession().getNamespace()).list();
    }
}
