package org.arquillian.cube.kubernetes.fabric8.impl.label;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.arquillian.cube.impl.util.IOUtil;
import org.arquillian.cube.kubernetes.api.LabelProvider;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by iocanel on 8/1/16.
 */
public class Fabric8LabelProvider implements LabelProvider {

    @Inject
    Instance<KubernetesClient> client;

    @Override
    public Map<String, String> getLabels() {
        Map<String, String> labels = new HashMap<String, String>();
        labels.put("project", client.get().getNamespace());
        labels.put("framework", "arquillian");
        labels.put("provider", "fabric8");
        labels.put("component", "integrationTest");
        return labels;
    }
}
