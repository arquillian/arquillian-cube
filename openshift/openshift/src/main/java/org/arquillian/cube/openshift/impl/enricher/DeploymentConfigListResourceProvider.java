package org.arquillian.cube.openshift.impl.enricher;

import io.fabric8.kubernetes.api.model.extensions.DeploymentList;
import java.lang.annotation.Annotation;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

/**
 * A {@link ResourceProvider} for {@link DeploymentList}.
 * It refers to deployment configs that have been created during the current session.
 */
public class DeploymentConfigListResourceProvider extends AbstractOpenshiftResourceProvider {

    @Override
    public boolean canProvide(Class<?> type) {
        return DeploymentList.class.isAssignableFrom(type);
    }

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        return getOpenshiftClient().deploymentConfigs().inNamespace(getSession().getNamespace()).list();
    }
}
