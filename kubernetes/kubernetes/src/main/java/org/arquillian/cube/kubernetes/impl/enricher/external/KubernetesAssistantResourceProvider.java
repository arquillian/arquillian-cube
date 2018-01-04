package org.arquillian.cube.kubernetes.impl.enricher.external;

import org.arquillian.cube.kubernetes.impl.KubernetesAssistant;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

import java.lang.annotation.Annotation;

public class KubernetesAssistantResourceProvider implements ResourceProvider {

    @Inject
    private Instance<KubernetesAssistant> kubernetesAssistantInstance;

    @Override
    public boolean canProvide(Class<?> type) {
        return KubernetesAssistant.class.getName().equals(type.getName());
    }

    @Override
    public Object lookup(ArquillianResource arquillianResource, Annotation... annotations) {
        KubernetesAssistant kubernetesAssistant = this.kubernetesAssistantInstance.get();

        if (kubernetesAssistant == null) {
            throw new IllegalStateException("Unable to inject KubernetesAssistant into test.");
        }

        return kubernetesAssistant;
    }
}
