package org.arquillian.cube.openshift.impl.client;

import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

public class OpenShiftAssistantCreator {

    @Inject
    @ApplicationScoped
    private InstanceProducer<OpenShiftAssistant> openShiftAssistantInstanceProducer;

    public void createOpenShiftAssistant(@Observes OpenShiftClient openShiftClient) {
        OpenShiftAssistant openShiftAssistant = new OpenShiftAssistant(openShiftClient.getClientExt(), openShiftClient.getNamespace());
        openShiftAssistantInstanceProducer.set(openShiftAssistant);
    }

}
