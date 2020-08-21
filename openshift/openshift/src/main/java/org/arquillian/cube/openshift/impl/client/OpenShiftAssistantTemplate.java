package org.arquillian.cube.openshift.impl.client;

import io.fabric8.kubernetes.api.model.v4_10.KubernetesList;
import io.fabric8.openshift.api.model.v4_10.DoneableTemplate;
import io.fabric8.openshift.api.model.v4_10.Template;
import io.fabric8.openshift.clnt.v4_10.OpenShiftClient;
import io.fabric8.openshift.clnt.v4_10.ParameterValue;
import io.fabric8.openshift.clnt.v4_10.dsl.TemplateResource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class OpenShiftAssistantTemplate {

    private final OpenShiftClient client;

    private URL templateURL;

    private HashMap<String, String> parameterValues = new HashMap<>();

    OpenShiftAssistantTemplate(URL templateURL, OpenShiftClient client) {
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

    private KubernetesList processTemplate(URL templateURL, HashMap<String, String> parameterValues) throws IOException {
        List<ParameterValue> list = new ArrayList<>();

        try (InputStream stream = templateURL.openStream()) {
            TemplateResource<Template, KubernetesList, DoneableTemplate> templateHandle =
                client.templates().inNamespace(client.getNamespace()).load(stream);

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
