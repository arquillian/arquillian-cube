package org.arquillian.cube.kubernetes.fabric8.impl.label;

import io.fabric8.kubernetes.clnt.v2_6.KubernetesClient;

import org.arquillian.cube.kubernetes.api.LabelProvider;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.spi.Validate;

import java.util.HashMap;
import java.util.Map;

public class Fabric8LabelProvider implements LabelProvider {

    @Inject
    Instance<KubernetesClient> client;

    LabelProvider delegate;

    @Override
    public Map<String, String> getLabels() {
       return toImmutable().getLabels();
    }

    @Override
    public LabelProvider toImmutable() {
        if (delegate != null) {
            return delegate;
        }
        synchronized(this) {
            if (delegate == null) {
                delegate = new ImmutableFabric8LabelProvider(client.get());
            }
        }
        return delegate;
    }

    public static class ImmutableFabric8LabelProvider implements LabelProvider {

        private final KubernetesClient client;

        private ImmutableFabric8LabelProvider(KubernetesClient client) {
            Validate.notNull(client, "A KubernetesClient instance is required.");
            this.client = client;
        }

        @Override
        public Map<String, String> getLabels() {
            Map<String, String> labels = new HashMap<String, String>();
            labels.put("project", client.getNamespace());
            labels.put("framework", "arquillian");
            labels.put("provider", "fabric8");
            labels.put("component", "integrationTest");
            return labels;
        }

        @Override
        public LabelProvider toImmutable() {
            return this;
        }
    }
}
