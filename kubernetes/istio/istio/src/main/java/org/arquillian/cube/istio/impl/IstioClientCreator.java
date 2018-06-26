package org.arquillian.cube.istio.impl;

import io.fabric8.kubernetes.clnt.v4_0.KubernetesClient;
import me.snowdrop.istio.client.IstioClient;
import me.snowdrop.istio.client.KubernetesAdapter;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

public class IstioClientCreator {

    @Inject
    @ApplicationScoped
    InstanceProducer<IstioClient> istioClientInstanceProducer;

    public void createIstioClient(@Observes final KubernetesClient client) {

        KubernetesAdapter adapter = new KubernetesAdapter(client);
        IstioClient istioClient = new IstioClient(adapter);

        istioClientInstanceProducer.set(istioClient);

    }

}
