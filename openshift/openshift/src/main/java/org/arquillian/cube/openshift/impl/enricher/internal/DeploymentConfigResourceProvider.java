package org.arquillian.cube.openshift.impl.enricher.internal;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

import io.fabric8.openshift.api.model.v2_6.DeploymentConfig;
import io.fabric8.openshift.api.model.v2_6.DeploymentConfigList;

import org.arquillian.cube.openshift.impl.enricher.AbstractOpenshiftResourceProvider;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

/**
 * A {@link ResourceProvider} for {@link DeploymentConfig}.
 * It refers to deployment configs that have been created during the current session.
 */
public class DeploymentConfigResourceProvider extends AbstractOpenshiftResourceProvider {
    @Override
    public boolean canProvide(Class<?> type) {
        return DeploymentConfig.class.isAssignableFrom(type);
    }

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        String name = getName(qualifiers);
        if (name != null) {
            return getOpenshiftClient().deploymentConfigs()
                .inNamespace(getSession().getNamespace())
                .withName(getName(qualifiers))
                .get();
        }

        // Gets the first deployment config that matches the labels.
        Map<String, String> labels = getLabels(qualifiers);
        DeploymentConfigList list = getOpenshiftClient().deploymentConfigs().inNamespace(getSession().getNamespace()).withLabels(labels).list();
        List<DeploymentConfig> deploymentConfigs = list.getItems();
        if( !deploymentConfigs.isEmpty() ) {
            return deploymentConfigs.get(0);
        }

        return null;
    }
}
