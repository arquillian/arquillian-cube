package org.arquillian.cube.openshift.impl.enricher.internal;

import io.fabric8.kubernetes.api.model.v2_2.extensions.DeploymentList;
import java.lang.annotation.Annotation;

import org.arquillian.cube.openshift.impl.enricher.AbstractOpenshiftResourceProvider;
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
