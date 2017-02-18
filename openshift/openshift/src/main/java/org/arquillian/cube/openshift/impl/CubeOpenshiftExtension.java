package org.arquillian.cube.openshift.impl;

import org.arquillian.cube.impl.client.enricher.StandaloneCubeUrlResourceProvider;
import org.arquillian.cube.kubernetes.api.NamespaceService;
import org.arquillian.cube.kubernetes.impl.enricher.UrlResourceProvider;
import org.arquillian.cube.kubernetes.impl.namespace.DefaultNamespaceService;
import org.arquillian.cube.openshift.impl.client.CubeOpenShiftConfigurator;
import org.arquillian.cube.openshift.impl.client.CubeOpenShiftRegistrar;
import org.arquillian.cube.openshift.impl.client.OpenShiftClientCreator;
import org.arquillian.cube.openshift.impl.client.OpenShiftSuiteLifecycleController;
import org.arquillian.cube.openshift.impl.enricher.DeploymentConfigListResourceProvider;
import org.arquillian.cube.openshift.impl.enricher.DeploymentConfigResourceProvider;
import org.arquillian.cube.openshift.impl.namespace.OpenshiftNamespaceService;
import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

public class CubeOpenshiftExtension implements LoadableExtension {

    @Override
    public void register(ExtensionBuilder builder) {
        builder.observer(OpenShiftClientCreator.class)
               .observer(CubeOpenShiftConfigurator.class)
               .observer(CubeOpenShiftRegistrar.class)
               .observer(OpenShiftSuiteLifecycleController.class)

                .service(ResourceProvider.class, DeploymentConfigResourceProvider.class)
                .service(ResourceProvider.class, DeploymentConfigListResourceProvider.class)

                .override(ResourceProvider.class, StandaloneCubeUrlResourceProvider.class, UrlResourceProvider.class)
                .override(NamespaceService.class, DefaultNamespaceService.class, OpenshiftNamespaceService.class);
    }

}
