/*
 * JBoss, Home of Professional Open Source
 * Copyright 2016 Red Hat Inc. and/or its affiliates and other
 * contributors as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a full listing of
 * individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.arquillian.cube.openshift.impl;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.arquillian.cube.kubernetes.api.Configuration;
import org.arquillian.cube.openshift.api.Template;
import org.arquillian.cube.openshift.api.model.DeploymentConfig;
import org.arquillian.cube.openshift.api.model.OpenShiftResource;
import org.arquillian.cube.openshift.impl.client.CubeOpenShiftConfiguration;
import org.arquillian.cube.openshift.impl.client.OpenShiftClient;
import org.arquillian.cube.openshift.impl.adapter.OpenShiftAdapter;
import org.arquillian.cube.openshift.impl.resources.OpenShiftResourceFactory;
import org.arquillian.cube.openshift.impl.utils.Operator;
import org.arquillian.cube.openshift.impl.utils.ParamValue;
import org.arquillian.cube.openshift.impl.utils.StringResolver;
import org.arquillian.cube.openshift.impl.utils.Strings;
import org.arquillian.cube.openshift.impl.utils.TemplateUtils;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.event.container.AfterStart;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.arquillian.test.spi.annotation.ClassScoped;
import org.jboss.arquillian.test.spi.event.suite.After;
import org.jboss.arquillian.test.spi.event.suite.AfterClass;
import org.jboss.arquillian.test.spi.event.suite.Before;
import org.jboss.arquillian.test.spi.event.suite.BeforeClass;

import static org.arquillian.cube.openshift.impl.utils.TemplateUtils.addParameterValues;

/**
 * CEEnvironmentProcessor
 * <p>
 * Temporary class to handle @Template, @Templates, and @OpenShiftResource annotations on
 * test classes. Eventually, these will be migrated to Cube types, at which
 * point this will delegate to those for setup/teardown (via
 * StartCube/StopCube).
 *
 * @author Rob Cernich
 */
public class CEEnvironmentProcessor {

    private final Logger log = Logger.getLogger(CEEnvironmentProcessor.class.getName());
    private List<Template> templates = Collections.emptyList();

    public interface TemplateDetails {
        List<List<? extends OpenShiftResource>> getResources();
    }

    @Inject
    private Instance<Configuration> configurationInstance;

    @Inject
    @ClassScoped
    private InstanceProducer<TemplateDetails> templateDetailsProducer;

    /**
     * Create the environment as specified by @Template or
     * arq.extension.ce-cube.openshift.template.* properties.
     * <p>
     * In the future, this might be handled by starting application Cube
     * objects, e.g. CreateCube(application), StartCube(application)
     * <p>
     * Needs to fire before the containers are started.
     */
    public void createEnvironment(@Observes(precedence = 10) BeforeClass event, OpenShiftAdapter client,
        CubeOpenShiftConfiguration configuration) throws DeploymentException {
        final TestClass testClass = event.getTestClass();
        log.info(String.format("Creating environment for %s", testClass.getName()));
        OpenShiftResourceFactory.createResources(testClass.getName(), client, testClass.getJavaClass(),
            configuration.getProperties());
        processTemplateResources(testClass, client, configuration);
    }

    public void createOpenShiftResource(@Observes(precedence = 10) Before event, OpenShiftAdapter client,
        CubeOpenShiftConfiguration cubeOpenShiftConfiguration) {

        final TestClass testClass = event.getTestClass();
        final Method testMethod = event.getTestMethod();

        log.info(String.format("Creating environment for %s method %s", testClass.getName(), testMethod));

        OpenShiftResourceFactory.createResources(testClass.getJavaClass(), client, testMethod, cubeOpenShiftConfiguration.getProperties());

    }

    public void deleteOpenShiftResource(@Observes(precedence = -10) After event, OpenShiftAdapter client) {

        final TestClass testClass = event.getTestClass();
        final Method testMethod = event.getTestMethod();

        log.info(String.format("Deleting environment for %s method %s", testClass.getName(), testMethod.getName()));

        OpenShiftResourceFactory.deleteReosurces(testClass.getJavaClass(), testMethod, client);

    }

    /**
     * Instantiates the templates specified by @Template within @Templates
     */
    private void processTemplateResources(TestClass testClass, OpenShiftAdapter client, CubeOpenShiftConfiguration configuration)
        throws DeploymentException {
        List<? extends OpenShiftResource> resources;
        final List<List<? extends OpenShiftResource>> RESOURCES = new ArrayList<List<? extends OpenShiftResource>>();
        templates = OpenShiftResourceFactory.getTemplates(testClass.getJavaClass());
        boolean sync_instantiation = OpenShiftResourceFactory.syncInstantiation(testClass.getJavaClass());

        /* Instantiate templates */
        for (Template template : templates) {
            resources = processTemplate(template, testClass, client, configuration);
            if (sync_instantiation) {
                /* synchronous template instantiation */
                RESOURCES.add(resources);
            } else {
                /* asynchronous template instantiation */
                try {
                    delay(client, resources);
                } catch (Throwable t) {
                    throw new DeploymentException(
                        "Error waiting for template resources to deploy: " + testClass.getName(), t);
                }
            }
        }
        templateDetailsProducer.set(() -> RESOURCES);
    }

    /**
     * Wait for the template resources to come up after the test container has
     * been started. This allows the test container and the template resources
     * to come up in parallel.
     */
    public void waitForDeployments(@Observes(precedence = -100) AfterStart event, OpenShiftAdapter client,
        TemplateDetails details, TestClass testClass, CubeOpenShiftConfiguration configuration, OpenShiftClient openshiftClient)
        throws Exception {
        if (testClass == null) {
            // nothing to do, since we're not in ClassScoped context
            return;
        }
        if (details == null) {
            log.warning(String.format("No environment for %s", testClass.getName()));
            return;
        }
        log.info(String.format("Waiting for environment for %s", testClass.getName()));
        try {
            for (List<? extends OpenShiftResource> resources : details.getResources()) {
                delay(client, resources);
            }
        } catch (Throwable t) {
            throw new DeploymentException("Error waiting for template resources to deploy: " + testClass.getName(), t);
        }
    }

    /**
     * Tear down the environment.
     * <p>
     * In the future, this might be handled by stopping application Cube
     * objects, e.g. StopCube(application), DestroyCube(application).
     */
    public void deleteEnvironment(@Observes(precedence = -10) AfterClass event, OpenShiftAdapter client,
        CubeOpenShiftConfiguration configuration) throws Exception {
        deleteEnvironment(event.getTestClass(), client, configuration);
    }

    private void deleteEnvironment(final TestClass testClass, OpenShiftAdapter client, CubeOpenShiftConfiguration configuration)
        throws Exception {
        StringResolver resolver;
        String templateURL;

        if (configuration.getCubeConfiguration().isNamespaceCleanupEnabled()) {
            log.info(String.format("Deleting environment for %s", testClass.getName()));
            for (Template template : templates) {
                // Delete pods and services related to each template
                resolver = Strings.createStringResolver(configuration.getProperties());
                templateURL = TemplateUtils.readTemplateUrl(template, configuration, false, resolver);

                client.deleteTemplate(testClass.getName() + templateURL);
            }
            OpenShiftResourceFactory.deleteResources(testClass.getName(), client);
            additionalCleanup(client,
                Collections.singletonMap("test-case", testClass.getJavaClass().getSimpleName().toLowerCase()));
        } else {
            log.info(String.format("Ignoring cleanup for %s", testClass.getName()));
        }
    }

    private List<? extends OpenShiftResource> processTemplate(Template template, TestClass tc, OpenShiftAdapter client,
        CubeOpenShiftConfiguration configuration) throws DeploymentException {
        final StringResolver resolver = Strings.createStringResolver(configuration.getProperties());
        final String templateURL = TemplateUtils.readTemplateUrl(template, configuration, false, resolver);

        if (templateURL == null) {
            log.info(String.format("No template specified for %s", tc.getName()));
            return null;
        }

        final List<? extends OpenShiftResource> resources;
        try {
            final int replicas = TemplateUtils.readReplicas(tc);

            final Map<String, String> readLabels = TemplateUtils.readLabels(template, configuration, resolver);
            if (readLabels.isEmpty()) {
                log.warning(String.format("Empty labels for template: %s, namespace: %s", templateURL,
                    configuration.getNamespace()));
            }

            final Map<String, String> labels = new HashMap<>(readLabels);
            labels.put("test-case", tc.getJavaClass().getSimpleName().toLowerCase());

            if (TemplateUtils.executeProcessTemplate(template, configuration)) {
                List<ParamValue> values = new ArrayList<>();
                addParameterValues(values, TemplateUtils.readParameters(template, configuration, resolver), false);
                addParameterValues(values, System.getenv(), true);
                addParameterValues(values, System.getProperties(), true);
                values.add(new ParamValue("REPLICAS", String.valueOf(replicas))); // not yet supported

                log.info(String.format("Applying OpenShift template: %s", templateURL));
                try {
                    // class name + templateUrl is template key
                    resources =
                        client.processTemplateAndCreateResources(tc.getName() + templateURL, templateURL, values, labels);
                } catch (Exception e) {
                    deleteEnvironment(tc, client, configuration);
                    throw e;
                }
            } else {
                log.info(String.format("Ignoring template [%s] processing ...", templateURL));
                resources = Collections.emptyList();
            }

            return resources;
        } catch (Throwable t) {
            throw new DeploymentException("Cannot deploy template: " + templateURL, t);
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

    private void additionalCleanup(OpenShiftAdapter client, Map<String, String> labels) throws Exception {
        client.cleanRemnants(labels);
    }
}
