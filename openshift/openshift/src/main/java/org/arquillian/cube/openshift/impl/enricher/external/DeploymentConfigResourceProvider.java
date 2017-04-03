package org.arquillian.cube.openshift.impl.enricher.external;

import org.arquillian.cube.openshift.impl.enricher.AbstractOpenshiftResourceProvider;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

import java.lang.annotation.Annotation;

import io.fabric8.openshift.api.model.v2_2.DeploymentConfig;

/**
 * A {@link ResourceProvider} for {@link DeploymentConfig}.
 * It refers to deployment configs that have been created during the current session.
 */
public class DeploymentConfigResourceProvider extends AbstractOpenshiftResourceProvider {
    @Override
    public boolean canProvide(Class<?> type) {
        return internalToUserType(DeploymentConfig.class.getName()).equals(type.getName());
    }

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        return toUsersResource(getOpenshiftClient().deploymentConfigs()
            .inNamespace(getSession().getNamespace())
            .withName(getName(qualifiers))
            .get());
    }
}
