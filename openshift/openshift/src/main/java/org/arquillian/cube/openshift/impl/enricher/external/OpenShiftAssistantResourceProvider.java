package org.arquillian.cube.openshift.impl.enricher.external;

import org.arquillian.cube.openshift.impl.client.OpenShiftAssistant;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

import java.lang.annotation.Annotation;

public class OpenShiftAssistantResourceProvider implements ResourceProvider {

    @Inject
    private Instance<OpenShiftAssistant> openShiftAssistantInstance;

    @Override
    public boolean canProvide(Class<?> type) {
        return OpenShiftAssistant.class.isAssignableFrom(type);
    }

    @Override
    public Object lookup(ArquillianResource arquillianResource, Annotation... annotations) {
        OpenShiftAssistant openShiftAssistant = this.openShiftAssistantInstance.get();

        if (openShiftAssistant == null) {
            throw new IllegalStateException("Unable to inject OpenShift Assistant into test.");
        }

        return openShiftAssistant;
    }
}
