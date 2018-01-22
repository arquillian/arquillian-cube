package org.arquillian.cube.openshift.impl.resources;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.arquillian.cube.openshift.api.Template;
import org.arquillian.cube.openshift.impl.adapter.OpenShiftAdapter;
import org.arquillian.cube.openshift.impl.client.CubeOpenShiftConfiguration;
import org.jboss.arquillian.test.spi.TestClass;

import static org.arquillian.cube.openshift.impl.resources.OpenShiftResourceFactory.deleteEnvironment;

public class ClassTemplateProcessor extends TemplateProcessor<Class<?>> {

    private static final Logger LOGGER = Logger.getLogger(ClassTemplateProcessor.class.getName());

    public ClassTemplateProcessor(OpenShiftAdapter openShiftAdapter,
        CubeOpenShiftConfiguration configuration, TestClass testClass) {
        this.openShiftAdapter = openShiftAdapter;
        this.configuration = configuration;
        this.testClass = testClass;
    }

    public List<Template> getTemplates() {
        return templates;
    }

    @Override
    protected String templateKeyPrefix() {
        return testClass.getName();
    }

    @Override
    protected String asynchronousDelayErrorMessage() {
        return "Error waiting for template resources to deploy: " + testClass.getName();
    }

    @Override
    protected String noTemplateMessage() {
        return String.format("No template specified for %s", testClass.getName());
    }

    @Override
    protected Class<?> getType() {
        return testClass.getJavaClass();
    }

    @Override
    protected Map<String, String> scopeLabels() {
        return Collections.singletonMap("test-case", testClass.getJavaClass().getSimpleName().toLowerCase());
    }

    @Override
    protected void handleExceptionForCreatingResource() throws Exception {
        deleteEnvironment(testClass, openShiftAdapter, configuration, templates);
    }

    @Override
    protected Logger logger() {
        return LOGGER;
    }
}
