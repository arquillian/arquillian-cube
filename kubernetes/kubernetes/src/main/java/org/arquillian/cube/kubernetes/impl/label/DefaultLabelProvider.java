package org.arquillian.cube.kubernetes.impl.label;

import io.fabric8.kubernetes.clnt.v2_6.KubernetesClient;
import java.util.HashMap;
import java.util.Map;
import org.arquillian.cube.kubernetes.api.LabelProvider;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.spi.Validate;

public class DefaultLabelProvider implements LabelProvider {

    @Inject
    Instance<KubernetesClient> client;

    private LabelProvider delegate;

    @Override
    public Map<String, String> getLabels() {
        return delegate.getLabels();
    }

    @Override
    public LabelProvider toImmutable() {
        if (delegate != null) {
            return delegate;
        }
        synchronized (this) {
            if (delegate == null) {
                delegate = new ImmutableLabelProvider(client.get());
            }
        }
        return delegate;
    }

    public static class ImmutableLabelProvider implements LabelProvider {

        private final KubernetesClient client;

        private ImmutableLabelProvider(KubernetesClient client) {
            Validate.notNull(client, "A KubernetesClient instance is required.");
            this.client = client;
        }

        @Override
        public Map<String, String> getLabels() {
            Map<String, String> labels = new HashMap<String, String>();
            labels.put("project", client.getNamespace());
            labels.put("framework", "arquillian");
            labels.put("component", "integrationTest");
            return labels;
        }

        @Override
        public LabelProvider toImmutable() {
            return this;
        }
    }
}
