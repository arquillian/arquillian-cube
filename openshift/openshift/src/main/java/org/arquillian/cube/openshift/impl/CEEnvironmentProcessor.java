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
import java.util.List;
import java.util.logging.Logger;
import org.arquillian.cube.kubernetes.api.Configuration;
import org.arquillian.cube.openshift.api.model.OpenShiftResource;
import org.arquillian.cube.openshift.impl.adapter.OpenShiftAdapter;
import org.arquillian.cube.openshift.impl.client.CubeOpenShiftConfiguration;
import org.arquillian.cube.openshift.impl.resources.ClassTemplateProcessor;
import org.arquillian.cube.openshift.impl.resources.MethodTemplateProcessor;
import org.arquillian.cube.openshift.impl.resources.OpenShiftResourceFactory;
import org.arquillian.cube.openshift.impl.resources.TemplateProcessor;
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

import static org.arquillian.cube.openshift.impl.resources.OpenShiftResourceFactory.createResourceKey;

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

    public interface TemplateDetails {
        List<List<? extends OpenShiftResource>> getResources();
    }

    @Inject
    private Instance<Configuration> configurationInstance;

    @Inject
    @ClassScoped
    private InstanceProducer<TemplateDetails> templateDetailsProducer;

    private TemplateProcessor classTemplateProcessor;
    private TemplateProcessor methodTemplateProcessor;

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
        CubeOpenShiftConfiguration cubeOpenShiftConfiguration) {
        final TestClass testClass = event.getTestClass();
        log.info(String.format("Creating environment for %s", testClass.getName()));
        OpenShiftResourceFactory.createResources(testClass.getName(), client, testClass.getJavaClass(),
            cubeOpenShiftConfiguration.getProperties());
        classTemplateProcessor = new ClassTemplateProcessor(client, cubeOpenShiftConfiguration, testClass);
        final List<List<? extends OpenShiftResource>> templateResources =
            classTemplateProcessor.processTemplateResources();
        templateDetailsProducer.set(() -> templateResources);
    }

    public void createOpenShiftResource(@Observes(precedence = 10) Before event, OpenShiftAdapter client,
        CubeOpenShiftConfiguration cubeOpenShiftConfiguration) {

        final TestClass testClass = event.getTestClass();
        final Method testMethod = event.getTestMethod();

        log.info(String.format("Creating environment for %s method %s", testClass.getName(), testMethod));

        OpenShiftResourceFactory.createResources(testClass.getJavaClass(), client, testMethod, cubeOpenShiftConfiguration.getProperties());
        methodTemplateProcessor = new MethodTemplateProcessor(client, cubeOpenShiftConfiguration, testClass, testMethod);
        methodTemplateProcessor.processTemplateResources();
    }

    public void deleteOpenShiftResource(@Observes(precedence = -10) After event, OpenShiftAdapter client, CubeOpenShiftConfiguration cubeOpenShiftConfiguration)
        throws Exception {

        final TestClass testClass = event.getTestClass();
        final Method testMethod = event.getTestMethod();
        final String templateKeyPrefix = createResourceKey(testClass.getJavaClass(), testMethod);
        log.info(String.format("Deleting environment for %s method %s", testClass.getName(), testMethod.getName()));

        OpenShiftResourceFactory.deleteResources(testClass.getJavaClass(), testMethod, client);
        OpenShiftResourceFactory.deleteTemplates(templateKeyPrefix, methodTemplateProcessor.getTemplates(), client, cubeOpenShiftConfiguration);

    }

    /**
     * Tear down the environment.
     * <p>
     * In the future, this might be handled by stopping application Cube
     * objects, e.g. StopCube(application), DestroyCube(application).
     */
    public void deleteEnvironment(@Observes(precedence = -10) AfterClass event, OpenShiftAdapter client,
        CubeOpenShiftConfiguration configuration) throws Exception {
        OpenShiftResourceFactory.deleteEnvironment(event.getTestClass(), client, configuration, classTemplateProcessor.getTemplates());
    }

}
