package org.arquillian.cube.openshift.impl.enricher.external;

import java.lang.annotation.Annotation;

import io.fabric8.openshift.api.model.v2_6.DeploymentConfig;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

/**
 * A {@link ResourceProvider} for {@link DeploymentConfig}.
 * It refers to deployment configs that have been created during the current session.
 */
public class DeploymentConfigResourceProvider extends org.arquillian.cube.openshift.impl.enricher.internal.DeploymentConfigResourceProvider {
    @Override
    public boolean canProvide(Class<?> type) {
        return internalToUserType(DeploymentConfig.class.getName()).equals(type.getName());
    }

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        return toUsersResource(super.lookup(resource, qualifiers));
    }
}
