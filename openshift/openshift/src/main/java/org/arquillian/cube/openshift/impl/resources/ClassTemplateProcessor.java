package org.arquillian.cube.openshift.impl.resources;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.arquillian.cube.openshift.api.Template;
import org.arquillian.cube.openshift.api.model.OpenShiftResource;
import org.arquillian.cube.openshift.impl.adapter.OpenShiftAdapter;
import org.arquillian.cube.openshift.impl.client.CubeOpenShiftConfiguration;
import org.arquillian.cube.openshift.impl.utils.ParamValue;
import org.arquillian.cube.openshift.impl.utils.StringResolver;
import org.arquillian.cube.openshift.impl.utils.Strings;
import org.arquillian.cube.openshift.impl.utils.TemplateUtils;
import org.jboss.arquillian.test.spi.TestClass;

import static org.arquillian.cube.openshift.impl.resources.OpenShiftResourceFactory.deleteEnvironment;

public class ClassTemplateProcessor implements TemplateProcessor {

    private static final Logger log = Logger.getLogger(ClassTemplateProcessor.class.getName());

    private final OpenShiftAdapter openShiftAdapter;
    private final CubeOpenShiftConfiguration configuration;
    private final TestClass testClass;

    private List<Template> templates = Collections.emptyList();

    public ClassTemplateProcessor(OpenShiftAdapter openShiftAdapter,
        CubeOpenShiftConfiguration configuration, TestClass testClass) {
        this.openShiftAdapter = openShiftAdapter;
        this.configuration = configuration;
        this.testClass = testClass;
    }

    public List<Template> getTemplates() {
        return templates;
    }

    /**
     * Instantiates the templates specified by @Template within @Templates
     */
    public List<List<? extends OpenShiftResource>> processTemplateResources() {
        List<? extends OpenShiftResource> resources;
        final List<List<? extends OpenShiftResource>> RESOURCES = new ArrayList<>();
        templates = OpenShiftResourceFactory.getTemplates(testClass.getJavaClass());
        boolean sync_instantiation = OpenShiftResourceFactory.syncInstantiation(testClass.getJavaClass());

        /* Instantiate templates */
        for (Template template : templates) {
            resources = processTemplate(template);
            if (sync_instantiation) {
                /* synchronous template instantiation */
                RESOURCES.add(resources);
            } else {
                /* asynchronous template instantiation */
                try {
                    delay(openShiftAdapter, resources);
                } catch (Throwable t) {
                    throw new IllegalArgumentException(
                        "Error waiting for template resources to deploy: " + testClass.getName(), t);
                }
            }
        }

        return RESOURCES;
    }


    private List<? extends OpenShiftResource> processTemplate(Template template) {
        final StringResolver resolver = Strings.createStringResolver(configuration.getProperties());
        final String templateURL = TemplateUtils.readTemplateUrl(template, configuration, false, resolver);

        if (templateURL == null) {
            log.info(String.format("No template specified for %s",testClass.getName()));
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
            labels.put("test-case", testClass.getJavaClass().getSimpleName().toLowerCase());

            if (TemplateUtils.executeProcessTemplate(template, configuration)) {
                final Map<String, String> templateParameters =
                    TemplateUtils.readParameters(template, configuration, resolver);
                final List<ParamValue> values = addParameterValues(templateParameters, testClass);

                log.info(String.format("Applying OpenShift template: %s", templateURL));
                try {
                    // class name + templateUrl is template key
                    resources = openShiftAdapter.processTemplateAndCreateResources(testClass.getName() + templateURL,
                        templateURL, values, labels);
                } catch (Exception e) {
                    deleteEnvironment(testClass, openShiftAdapter, configuration, templates);
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

}
