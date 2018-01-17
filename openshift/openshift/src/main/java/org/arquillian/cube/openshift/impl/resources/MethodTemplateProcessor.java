package org.arquillian.cube.openshift.impl.resources;

import java.lang.reflect.Method;
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

import static org.arquillian.cube.openshift.impl.resources.OpenShiftResourceFactory.additionalCleanup;
import static org.arquillian.cube.openshift.impl.resources.OpenShiftResourceFactory.createResourceKey;
import static org.arquillian.cube.openshift.impl.resources.OpenShiftResourceFactory.deleteTemplates;

public class MethodTemplateProcessor implements TemplateProcessor {

    private static final Logger log = Logger.getLogger(MethodTemplateProcessor.class.getName());

    private final OpenShiftAdapter openShiftAdapter;
    private final CubeOpenShiftConfiguration configuration;
    private final TestClass testClass;
    private final Method testMethod;

    private List<Template> templates = Collections.emptyList();

    public MethodTemplateProcessor(OpenShiftAdapter openShiftAdapter,
        CubeOpenShiftConfiguration configuration, TestClass testClass, Method testMethod) {
        this.openShiftAdapter = openShiftAdapter;
        this.configuration = configuration;
        this.testClass = testClass;
        this.testMethod = testMethod;
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
        templates = OpenShiftResourceFactory.getTemplates(testMethod);
        boolean sync_instantiation = OpenShiftResourceFactory.syncInstantiation(testMethod);

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
                        String.format("Error waiting for template resources to deploy from class %s method %s",
                            testClass.getName(), testMethod.getName()), t);
                }
            }
        }

        return RESOURCES;
    }

    private List<? extends OpenShiftResource> processTemplate(Template template) {
        final StringResolver resolver = Strings.createStringResolver(configuration.getProperties());
        final String templateURL = TemplateUtils.readTemplateUrl(template, configuration, false, resolver);

        if (templateURL == null) {
            log.info(
                String.format("No template specified for class %s method %s", testClass.getName(), testMethod.getName()));
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
            final String testMethodLabel = testMethod.getName().toLowerCase();
            labels.put("test-method", testMethodLabel);

            if (TemplateUtils.executeProcessTemplate(template, configuration)) {
                final Map<String, String> templateParameters =
                    TemplateUtils.readParameters(template, configuration, resolver);
                final List<ParamValue> values = addParameterValues(templateParameters, testClass);

                log.info(String.format("Applying OpenShift template: %s", templateURL));

                final String templateKeyPrefix = createResourceKey(testClass.getJavaClass(), testMethod);
                try {
                    // class name + _ + method name + templateUrl is template key
                    resources =
                        openShiftAdapter.processTemplateAndCreateResources(templateKeyPrefix + templateURL, templateURL,
                            values, labels);
                } catch (Exception e) {
                    deleteTemplates(templateKeyPrefix, templates, openShiftAdapter, configuration);
                    additionalCleanup(openShiftAdapter, Collections.singletonMap("test-method", testMethodLabel));
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
