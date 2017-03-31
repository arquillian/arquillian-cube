package org.arquillian.cube.kubernetes.impl.locator;

import org.arquillian.cube.kubernetes.api.Configuration;
import org.arquillian.cube.kubernetes.api.KubernetesResourceLocator;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.ServiceLoader;

public class KubernetesResourceLocatorRegistar {

    @Inject
    private Instance<ServiceLoader> serviceLoader;

    @Inject @ApplicationScoped
    InstanceProducer<KubernetesResourceLocator> kubernetesResourceLocator;

    public void install(@Observes(precedence = 100) Configuration configuration) {
        kubernetesResourceLocator.set(serviceLoader.get().onlyOne(KubernetesResourceLocator.class, DefaultKubernetesResourceLocator.class));

    }
}
