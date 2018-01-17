package org.arquillian.cube.openshift.impl.resources;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.arquillian.cube.openshift.api.Template;
import org.arquillian.cube.openshift.api.model.DeploymentConfig;
import org.arquillian.cube.openshift.api.model.OpenShiftResource;
import org.arquillian.cube.openshift.impl.adapter.OpenShiftAdapter;
import org.arquillian.cube.openshift.impl.utils.Operator;
import org.arquillian.cube.openshift.impl.utils.ParamValue;
import org.arquillian.cube.openshift.impl.utils.TemplateUtils;

public interface TemplateProcessor {

    List<List<? extends OpenShiftResource>> processTemplateResources();

    List<Template> getTemplates();

    default void delay(OpenShiftAdapter client, final List<? extends OpenShiftResource> resources) throws Exception {
        for (OpenShiftResource resource : resources) {
            if (resource instanceof DeploymentConfig) {
                final DeploymentConfig dc = (DeploymentConfig) resource;
                client.delay(dc.getSelector(), dc.getReplicas(), Operator.EQUAL);
            }
        }
    }

    default <T> List<ParamValue> addParameterValues(Map<String, String> templateParameters, T type) {
        List<ParamValue> values = new ArrayList<>();
        final int replicas = TemplateUtils.readReplicas(type);

        TemplateUtils.addParameterValues(values, templateParameters, false);
        TemplateUtils.addParameterValues(values, System.getenv(), true);
        TemplateUtils.addParameterValues(values, System.getProperties(), true);
        values.add(new ParamValue("REPLICAS", String.valueOf(replicas))); // not yet supported

        return values;
    }
}
