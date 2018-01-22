package org.arquillian.cube.openshift.impl.resources;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.arquillian.cube.openshift.api.Template;
import org.arquillian.cube.openshift.impl.adapter.OpenShiftAdapter;
import org.arquillian.cube.openshift.impl.client.CubeOpenShiftConfiguration;
import org.jboss.arquillian.test.spi.TestClass;

import static org.arquillian.cube.openshift.impl.resources.OpenShiftResourceFactory.additionalCleanup;
import static org.arquillian.cube.openshift.impl.resources.OpenShiftResourceFactory.createResourceKey;
import static org.arquillian.cube.openshift.impl.resources.OpenShiftResourceFactory.deleteTemplates;

public class MethodTemplateProcessor extends TemplateProcessor<Method> {

    private static final Logger LOGGER = Logger.getLogger(MethodTemplateProcessor.class.getName());

    private final Method testMethod;

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

    @Override
    protected String templateKeyPrefix() {
        // class name + _ + method name  is template key Prefix
        return createResourceKey(testClass.getJavaClass(), testMethod);
    }

    @Override
    protected String asynchronousDelayErrorMessage() {
        return String.format("Error waiting for template resources to deploy from class %s method %s",
            testClass.getName(), testMethod.getName());
    }

    @Override
    protected String noTemplateMessage() {
        return String.format("No template specified for class %s method %s", testClass.getName(), testMethod.getName());
    }

    @Override
    protected Method getType() {
        return testMethod;
    }

    @Override
    protected Map<String, String> scopeLabels() {
        final String testMethodLabel = testMethod.getName().toLowerCase();
        return Collections.singletonMap("test-method", testMethodLabel);
    }

    @Override
    protected void handleExceptionForCreatingResource() throws Exception {
        deleteTemplates(templateKeyPrefix(), templates, openShiftAdapter, configuration);
        additionalCleanup(openShiftAdapter, scopeLabels());
    }

    @Override
    protected Logger logger() {
        return LOGGER;
    }
}
