package org.arquillian.cube.openshift.impl;

import org.arquillian.cube.kubernetes.api.NamespaceService;
import org.arquillian.cube.kubernetes.impl.namespace.DefaultNamespaceService;
import org.arquillian.cube.openshift.impl.client.CubeOpenShiftConfigurator;
import org.arquillian.cube.openshift.impl.client.CubeOpenShiftRegistrar;
import org.arquillian.cube.openshift.impl.client.OpenShiftClientCreator;
import org.arquillian.cube.openshift.impl.client.OpenShiftSuiteLifecycleController;
import org.arquillian.cube.openshift.impl.namespace.OpenshiftNamespaceService;
import org.eclipse.persistence.internal.oxm.Namespace;
import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

public class CubeOpenshiftExtension implements LoadableExtension {

    @Override
    public void register(ExtensionBuilder builder) {
        builder.observer(OpenShiftClientCreator.class)
               .observer(CubeOpenShiftConfigurator.class)
               .observer(CubeOpenShiftRegistrar.class)
               .observer(OpenShiftSuiteLifecycleController.class)
               .override(NamespaceService.class, DefaultNamespaceService.class, OpenshiftNamespaceService.class);
    }

}
