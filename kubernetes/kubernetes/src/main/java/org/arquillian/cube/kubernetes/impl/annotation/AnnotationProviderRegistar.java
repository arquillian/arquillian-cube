package org.arquillian.cube.kubernetes.impl.annotation;

import org.arquillian.cube.kubernetes.api.AnnotationProvider;
import org.arquillian.cube.kubernetes.api.Configuration;
import org.arquillian.cube.kubernetes.api.LabelProvider;
import org.arquillian.cube.kubernetes.impl.label.DefaultLabelProvider;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.ServiceLoader;

public class AnnotationProviderRegistar {

    @Inject
    private Instance<ServiceLoader> serviceLoader;

    @Inject @ApplicationScoped
    InstanceProducer<AnnotationProvider> annotationProvider;

    public void install(@Observes(precedence = 100) Configuration configuration) {
        annotationProvider.set(serviceLoader.get().onlyOne(AnnotationProvider.class, DefaultAnnotationProvider.class));

    }
}
