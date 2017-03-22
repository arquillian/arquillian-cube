package org.arquillian.cube.kubernetes.impl.feedback;

import org.arquillian.cube.kubernetes.api.Configuration;
import org.arquillian.cube.kubernetes.api.FeedbackProvider;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.ServiceLoader;

public class FeedbackProviderServiceRegistar {

    @Inject
    private Instance<ServiceLoader> serviceLoader;

    @Inject @ApplicationScoped
    InstanceProducer<FeedbackProvider> namespaceService;

    public void install(@Observes(precedence = 100) Configuration configuration) {
        namespaceService.set(serviceLoader.get().onlyOne(FeedbackProvider.class, DefaultFeedbackProvider.class));

    }
}
