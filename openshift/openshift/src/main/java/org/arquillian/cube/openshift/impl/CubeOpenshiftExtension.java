package org.arquillian.cube.openshift.impl;

import org.arquillian.cube.impl.client.enricher.StandaloneCubeUrlResourceProvider;
import org.arquillian.cube.kubernetes.api.ConfigurationFactory;
import org.arquillian.cube.kubernetes.api.FeedbackProvider;
import org.arquillian.cube.kubernetes.api.KubernetesResourceLocator;
import org.arquillian.cube.kubernetes.api.NamespaceService;
import org.arquillian.cube.kubernetes.api.ResourceInstaller;
import org.arquillian.cube.kubernetes.impl.DefaultConfigurationFactory;
import org.arquillian.cube.kubernetes.impl.enricher.KuberntesServiceUrlResourceProvider;
import org.arquillian.cube.kubernetes.impl.feedback.DefaultFeedbackProvider;
import org.arquillian.cube.kubernetes.impl.install.DefaultResourceInstaller;
import org.arquillian.cube.kubernetes.impl.locator.DefaultKubernetesResourceLocator;
import org.arquillian.cube.kubernetes.impl.namespace.DefaultNamespaceService;
import org.arquillian.cube.openshift.impl.client.CubeOpenShiftConfigurationFactory;
import org.arquillian.cube.openshift.impl.client.CubeOpenShiftRegistrar;
import org.arquillian.cube.openshift.impl.client.OpenShiftClientCreator;
import org.arquillian.cube.openshift.impl.client.OpenShiftSuiteLifecycleController;
import org.arquillian.cube.openshift.impl.enricher.RouteURLEnricher;
import org.arquillian.cube.openshift.impl.enricher.internal.DeploymentConfigListResourceProvider;
import org.arquillian.cube.openshift.impl.enricher.internal.DeploymentConfigResourceProvider;
import org.arquillian.cube.openshift.impl.enricher.internal.OpenshiftClientResourceProvider;
import org.arquillian.cube.openshift.impl.feedback.OpenshiftFeedbackProvider;
import org.arquillian.cube.openshift.impl.install.OpenshiftResourceInstaller;
import org.arquillian.cube.openshift.impl.locator.OpenshiftKubernetesResourceLocator;
import org.arquillian.cube.openshift.impl.namespace.OpenshiftNamespaceService;
import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.arquillian.test.spi.TestEnricher;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

public class CubeOpenshiftExtension implements LoadableExtension {

    @Override
    public void register(ExtensionBuilder builder) {
        builder.observer(OpenShiftClientCreator.class)
            .observer(CubeOpenShiftRegistrar.class)
            .observer(OpenShiftSuiteLifecycleController.class)

            //internal
            .service(ResourceProvider.class, OpenshiftClientResourceProvider.class)
            .service(ResourceProvider.class, DeploymentConfigResourceProvider.class)
            .service(ResourceProvider.class, DeploymentConfigListResourceProvider.class)

            //External
            .service(ResourceProvider.class, org.arquillian.cube.openshift.impl.enricher.external.OpenshiftClientResourceProvider.class)
            .service(ResourceProvider.class, org.arquillian.cube.openshift.impl.enricher.external.DeploymentConfigResourceProvider.class)
            .service(ResourceProvider.class, org.arquillian.cube.openshift.impl.enricher.external.DeploymentConfigListResourceProvider.class)

            .service(TestEnricher.class, RouteURLEnricher.class)

            .override(ConfigurationFactory.class, DefaultConfigurationFactory.class,
                CubeOpenShiftConfigurationFactory.class)
            .override(ResourceProvider.class, StandaloneCubeUrlResourceProvider.class,
                KuberntesServiceUrlResourceProvider.class)
            .override(ResourceInstaller.class, DefaultResourceInstaller.class, OpenshiftResourceInstaller.class)
            .override(FeedbackProvider.class, DefaultFeedbackProvider.class, OpenshiftFeedbackProvider.class)
            .override(KubernetesResourceLocator.class, DefaultKubernetesResourceLocator.class,
                OpenshiftKubernetesResourceLocator.class)
            .override(NamespaceService.class, DefaultNamespaceService.class, OpenshiftNamespaceService.class);
    }
}
