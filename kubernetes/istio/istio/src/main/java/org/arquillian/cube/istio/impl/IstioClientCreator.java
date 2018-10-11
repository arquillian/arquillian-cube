package org.arquillian.cube.istio.impl;

import io.fabric8.kubernetes.clnt.v4_0.KubernetesClient;
import me.snowdrop.istio.client.DefaultIstioClient;
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

        IstioClient istioClient = new DefaultIstioClient(client.getConfiguration());
        istioClientInstanceProducer.set(istioClient);

    }

}
