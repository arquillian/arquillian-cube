package org.arquillian.cube.kubernetes.impl.resources;

import io.fabric8.kubernetes.api.model.v4_10.HasMetadata;
import io.fabric8.kubernetes.clnt.v4_10.KubernetesClient;

import java.io.InputStream;
import java.util.List;

public class KubernetesResourceHandle {

    private List<HasMetadata> hasMetadata;

    KubernetesResourceHandle(KubernetesClient client, InputStream content) {
        hasMetadata = client.load(content).createOrReplace();
    }

    public void delete(KubernetesClient client) {
        client.resourceList(hasMetadata).delete();
    }
}
