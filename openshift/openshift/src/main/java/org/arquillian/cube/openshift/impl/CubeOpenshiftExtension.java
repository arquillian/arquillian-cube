package org.arquillian.cube.openshift.impl;

import org.arquillian.cube.openshift.impl.client.CubeOpenShiftConfigurator;
import org.arquillian.cube.openshift.impl.client.CubeOpenShiftRegistrar;
import org.arquillian.cube.openshift.impl.client.OpenShiftClientCreator;
import org.arquillian.cube.openshift.impl.client.OpenShiftPortProxyController;
import org.arquillian.cube.openshift.impl.client.OpenShiftSuiteLifecycleController;
import org.jboss.arquillian.core.spi.LoadableExtension;

public class CubeOpenshiftExtension implements LoadableExtension {

    @Override
    public void register(ExtensionBuilder builder) {
        builder.observer(OpenShiftClientCreator.class)
               .observer(CubeOpenShiftConfigurator.class)
               .observer(CubeOpenShiftRegistrar.class)
               .observer(OpenShiftPortProxyController.class)
               .observer(OpenShiftSuiteLifecycleController.class);
    }

}
