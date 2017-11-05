package org.arquillian.cube.kubernetes.impl.label;

import org.arquillian.cube.kubernetes.api.Configuration;
import org.arquillian.cube.kubernetes.api.LabelProvider;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.ServiceLoader;

public class LabelProviderRegistar {

    @Inject @ApplicationScoped
    InstanceProducer<LabelProvider> labelProvider;
    @Inject
    private Instance<ServiceLoader> serviceLoader;

    public void install(@Observes(precedence = 100) Configuration configuration) {
        labelProvider.set(serviceLoader.get().onlyOne(LabelProvider.class, DefaultLabelProvider.class));
    }
}
