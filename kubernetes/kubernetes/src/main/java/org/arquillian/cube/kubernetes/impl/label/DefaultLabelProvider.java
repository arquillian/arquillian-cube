package org.arquillian.cube.kubernetes.impl.label;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.arquillian.cube.kubernetes.api.LabelProvider;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by iocanel on 8/1/16.
 */
public class DefaultLabelProvider implements LabelProvider {

    @Inject
    Instance<KubernetesClient> client;

    @Override
    public Map<String, String> getLabels() {
        Map<String, String> labels = new HashMap<String, String>();
        labels.put("project", client.get().getNamespace());
        labels.put("framework", "arquillian");
        labels.put("component", "integrationTest");
        return labels;
    }
}
