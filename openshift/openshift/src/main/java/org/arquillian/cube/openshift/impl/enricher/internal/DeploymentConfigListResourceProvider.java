package org.arquillian.cube.openshift.impl.enricher.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import io.fabric8.kubernetes.api.model.v2_6.extensions.DeploymentList;

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
        Map<String, String> labels = getLabels(qualifiers);
        if( labels.isEmpty() ) {
            return getOpenshiftClient().deploymentConfigs().inNamespace(getSession().getNamespace()).list();
        } else {
            return getOpenshiftClient().deploymentConfigs().inNamespace(getSession().getNamespace()).withLabels(labels).list();
        }
    }
}
