package org.arquillian.cube.istio.impl.enricher;

import java.lang.annotation.Annotation;
import org.arquillian.cube.istio.impl.IstioAssistant;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

public class IstioAssistantResourceProvider implements ResourceProvider {

    @Inject
    private Instance<IstioAssistant> istioAssistantInstance;

    @Override
    public boolean canProvide(Class<?> type) {
        return IstioAssistant.class.getName().equals(type.getName());
    }

    @Override
    public Object lookup(ArquillianResource arquillianResource, Annotation... annotations) {
        IstioAssistant istioAssistant = this.istioAssistantInstance.get();

        if (istioAssistant == null) {
            throw new IllegalStateException("Unable to inject IstioAssistant into test.");
        }

        return istioAssistant;
    }

}
