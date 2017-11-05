package org.arquillian.cube.openshift.impl.enricher.external;

import java.lang.annotation.Annotation;

import io.fabric8.kubernetes.api.model.v2_6.extensions.DeploymentList;
import io.fabric8.openshift.api.model.v2_6.DeploymentConfigList;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

/**
 * A {@link ResourceProvider} for {@link DeploymentList}.
 * It refers to deployment configs that have been created during the current session.
 */
public class DeploymentConfigListResourceProvider extends org.arquillian.cube.openshift.impl.enricher.internal.DeploymentConfigListResourceProvider {

    @Override
    public boolean canProvide(Class<?> type) {
        return internalToUserType(DeploymentConfigList.class.getName()).equals(type.getName());
    }

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        return toUsersResource(super.lookup(resource, qualifiers));
    }
}
