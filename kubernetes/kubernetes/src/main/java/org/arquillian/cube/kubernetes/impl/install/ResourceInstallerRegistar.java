package org.arquillian.cube.kubernetes.impl.install;

import org.arquillian.cube.kubernetes.api.Configuration;
import org.arquillian.cube.kubernetes.api.ResourceInstaller;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.ServiceLoader;

public class ResourceInstallerRegistar {

    @Inject @ApplicationScoped
    InstanceProducer<ResourceInstaller> resourceInstaller;
    @Inject
    private Instance<ServiceLoader> serviceLoader;

    public void install(@Observes(precedence = 100) Configuration configuration) {
        resourceInstaller.set(serviceLoader.get().onlyOne(ResourceInstaller.class, DefaultResourceInstaller.class));
    }
}
