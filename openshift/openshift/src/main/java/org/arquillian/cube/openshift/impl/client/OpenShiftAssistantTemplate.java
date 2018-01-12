package org.arquillian.cube.openshift.impl.client;

import io.fabric8.kubernetes.api.model.v3_1.KubernetesList;
import io.fabric8.kubernetes.clnt.v3_1.KubernetesClient;
import io.fabric8.openshift.api.model.v3_1.DoneableTemplate;
import io.fabric8.openshift.api.model.v3_1.Template;
import io.fabric8.openshift.clnt.v3_1.OpenShiftClient;
import io.fabric8.openshift.clnt.v3_1.ParameterValue;
import io.fabric8.openshift.clnt.v3_1.dsl.TemplateResource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class OpenShiftAssistantTemplate {

    private final KubernetesClient client;

    private String templateURL;

    private HashMap<String, String> parameterValues = new HashMap<>();

    OpenShiftAssistantTemplate(String templateURL, KubernetesClient client) {
        this.templateURL = templateURL;
        this.client = client;
    }

    /**
     * Stores template parameters for OpenShiftAssistantTemplate.
     *
     * @param name  template parameter name
     * @param value template parameter value
     */
    public OpenShiftAssistantTemplate parameter(String name, String value) {
        parameterValues.put(name, value);
        return this;
    }

    /**
     * Deploys application reading resources from specified TemplateURL.
     *
     * @throws IOException
     */
    public void deploy() throws IOException {
        KubernetesList list = processTemplate(templateURL, parameterValues);
        createResources(list);
    }

    private KubernetesList processTemplate(String templateURL, HashMap<String, String> parameterValues) throws IOException {
        final OpenShiftClient openShiftClient = client.adapt(OpenShiftClient.class);
        List<ParameterValue> list = new ArrayList<>();

        try (InputStream stream = new URL(templateURL).openStream()) {
            TemplateResource<Template, KubernetesList, DoneableTemplate> templateHandle =
                openShiftClient.templates().inNamespace(client.getNamespace()).load(stream);

            list.addAll(parameterValues.entrySet().stream()
                .map(entry -> new ParameterValue(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList()));

            return templateHandle.process(list.toArray(new ParameterValue[parameterValues.size()]));
        }
    }

    private KubernetesList createResources(KubernetesList list) {
        return client.lists().inNamespace(client.getNamespace()).create(list);
    }
}
