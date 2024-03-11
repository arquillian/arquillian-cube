package org.arquillian.cube.istio.impl;

import io.fabric8.istio.client.IstioClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

public class IstioClientCreator {

    @Inject
    @ApplicationScoped
    InstanceProducer<IstioClient> istioClientInstanceProducer;

    public void createIstioClient(@Observes final KubernetesClient client) {

        IstioClient istioClient = client.adapt(IstioClientAdapter.class);
        istioClientInstanceProducer.set(istioClient);

    }

}
