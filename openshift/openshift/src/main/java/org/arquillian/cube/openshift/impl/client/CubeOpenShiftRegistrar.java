package org.arquillian.cube.openshift.impl.client;

import org.arquillian.cube.openshift.impl.model.BuildablePodCube;
import org.arquillian.cube.openshift.impl.model.ServiceCube;
import org.arquillian.cube.spi.CubeRegistry;
import org.jboss.arquillian.core.api.Injector;
import org.jboss.arquillian.core.api.annotation.Observes;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;

public class CubeOpenShiftRegistrar {

    public void register(@Observes OpenShiftClient client, CubeRegistry registry, CubeOpenShiftConfiguration configuration, Injector injector) {
        Object model = configuration.getDefinitions();

        if (model instanceof KubernetesList) {
            KubernetesList list = (KubernetesList) model;
            for(HasMetadata meta : list.getItems()) {
                register(meta, registry, client, configuration, injector);
            }
        } else {
            register(model, registry, client, configuration, injector);
        }
    }

    private void register(Object model, CubeRegistry registry, OpenShiftClient client, CubeOpenShiftConfiguration configuration, Injector injector) {
        if (model instanceof Pod) {
            registry.addCube(injector.inject(new BuildablePodCube((Pod) model, client, configuration)));
        } else if (model instanceof Service) {
            registry.addCube(injector.inject(new ServiceCube((Service) model, client, configuration)));
        }
    }
}
