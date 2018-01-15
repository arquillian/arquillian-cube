package org.arquillian.cube.istio.impl;

import io.fabric8.kubernetes.clnt.v3_1.KubernetesClient;
import me.snowdrop.istio.api.model.IstioResource;
import me.snowdrop.istio.client.Adapter;
import me.snowdrop.istio.client.IstioClient;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

public class IstioClientCreator {

    @Inject
    @ApplicationScoped
    InstanceProducer<IstioClient> istioClientInstanceProducer;

    public void createIstioClient(@Observes final KubernetesClient client) {
        IstioClient istioClient = new IstioClient(
            new Adapter() {
                @Override
                public IstioResource createCustomResource(String crdName, IstioResource resource) {
                    return null;
                }
            }
        );

        istioClientInstanceProducer.set(istioClient);

    }

}
