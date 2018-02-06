package org.arquillian.cube.openshift.impl.resources;

import org.arquillian.cube.openshift.api.Template;
import org.arquillian.cube.openshift.api.model.DeploymentConfig;
import org.arquillian.cube.openshift.api.model.OpenShiftResource;
import org.arquillian.cube.openshift.impl.adapter.OpenShiftAdapter;
import org.arquillian.cube.openshift.impl.client.CubeOpenShiftConfiguration;
import org.arquillian.cube.openshift.impl.utils.Operator;
import org.arquillian.cube.openshift.impl.utils.ParamValue;
import org.arquillian.cube.openshift.impl.utils.StringResolver;
import org.arquillian.cube.openshift.impl.utils.Strings;
import org.arquillian.cube.openshift.impl.utils.TemplateUtils;
import org.jboss.arquillian.test.spi.TestClass;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public abstract class TemplateProcessor<T> {

    OpenShiftAdapter openShiftAdapter;
    protected CubeOpenShiftConfiguration configuration;
    TestClass testClass;
    List<Template> templates = Collections.emptyList();

    protected abstract T getType();

    protected abstract String templateKeyPrefix();

    public abstract List<Template> getTemplates();

    protected abstract String asynchronousDelayErrorMessage();

    protected abstract String noTemplateMessage();

    protected abstract Map<String, String> scopeLabels();

    protected abstract void handleExceptionForCreatingResource() throws Exception;

    protected abstract Logger logger();

    /**
     * Instantiates the templates specified by @Template within @Templates
     */
    public List<? super OpenShiftResource> processTemplateResources() {
        List<? extends OpenShiftResource> resources;
        final List<? super OpenShiftResource> processedResources = new ArrayList<>();
        templates = OpenShiftResourceFactory.getTemplates(getType());
        boolean sync_instantiation = OpenShiftResourceFactory.syncInstantiation(getType());

        /* Instantiate templates */
        for (Template template : templates) {
            resources = processTemplate(template);
            if (resources != null) {
                if (sync_instantiation) {
                /* synchronous template instantiation */
                    Collections.copy(processedResources, resources);
                } else {
                /* asynchronous template instantiation */
                    try {
                        delay(openShiftAdapter, resources);
                    } catch (Throwable t) {
                        throw new IllegalArgumentException(asynchronousDelayErrorMessage(), t);
                    }
                }
            }
        }

        return processedResources;
    }

    private List<? extends OpenShiftResource> processTemplate(Template template) {
        final StringResolver resolver = Strings.createStringResolver(configuration.getProperties());
        final InputStream templateURL = TemplateUtils.readTemplateUrl(template, testClass.getJavaClass(), configuration, false, resolver);
        final Logger log = logger();
        if (templateURL == null) {
            log.info(noTemplateMessage());
            return null;
        }

        final List<? extends OpenShiftResource> resources;
        try {
            final Map<String, String> readLabels = TemplateUtils.readLabels(template, configuration, resolver);
            if (readLabels.isEmpty()) {
                log.warning(String.format("Empty labels for template: %s, namespace: %s", templateURL,
                    configuration.getNamespace()));
            }

            final Map<String, String> labels = new HashMap<>(readLabels);
            labels.putAll(scopeLabels());

            if (TemplateUtils.executeProcessTemplate(template, configuration)) {
                final Map<String, String> templateParameters =
                    TemplateUtils.readParameters(template, configuration, resolver);
                final List<ParamValue> values = addParameterValues(templateParameters, getType());

                log.info(String.format("Applying OpenShift template: %s", templateURL));

                final String templateKeyPrefix = templateKeyPrefix();
                try {
                    resources =
                        openShiftAdapter.processTemplateAndCreateResources(templateKeyPrefix + templateURL, templateURL,
                            values, labels);
                } catch (Exception e) {
                    handleExceptionForCreatingResource();
                    throw e;
                }
            } else {
                log.info(String.format("Ignoring template [%s] processing ...", templateURL));
                resources = Collections.emptyList();
            }

            return resources;
        } catch (Throwable t) {
            throw new IllegalArgumentException("Cannot deploy template: " + templateURL, t);
        }
    }

    private void delay(OpenShiftAdapter client, final List<? extends OpenShiftResource> resources) throws Exception {
        for (OpenShiftResource resource : resources) {
            if (resource instanceof DeploymentConfig) {
                final DeploymentConfig dc = (DeploymentConfig) resource;
                client.delay(dc.getSelector(), dc.getReplicas(), Operator.EQUAL);
            }
        }
    }

    private List<ParamValue> addParameterValues(Map<String, String> templateParameters, T type) {
        List<ParamValue> values = new ArrayList<>();
        final int replicas = TemplateUtils.readReplicas(type);

        TemplateUtils.addParameterValues(values, templateParameters, false);
        TemplateUtils.addParameterValues(values, System.getenv(), true);
        TemplateUtils.addParameterValues(values, System.getProperties(), true);
        values.add(new ParamValue("REPLICAS", String.valueOf(replicas))); // not yet supported

        return values;
    }
}
