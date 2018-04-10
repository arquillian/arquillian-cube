package org.arquillian.cube.istio.impl;

import me.snowdrop.istio.client.IstioClient;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

public class IstioAssistantCreator {

    @Inject
    @ApplicationScoped
    private InstanceProducer<IstioAssistant> istioAssistantInstanceProducer;

    public void createIstioAssistant(@Observes IstioClient istioClient) {
        IstioAssistant istioAssistant = new IstioAssistant(istioClient);
        istioAssistantInstanceProducer.set(istioAssistant);
    }
}
