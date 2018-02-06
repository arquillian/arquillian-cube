/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015 Red Hat Inc. and/or its affiliates and other
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

package org.arquillian.cube.openshift.impl.resources;

import org.arquillian.cube.openshift.api.AddRoleToServiceAccount;
import org.arquillian.cube.openshift.api.OpenShiftResource;
import org.arquillian.cube.openshift.api.RoleBinding;
import org.arquillian.cube.openshift.api.Template;
import org.arquillian.cube.openshift.api.Templates;
import org.arquillian.cube.openshift.impl.adapter.OpenShiftAdapter;
import org.arquillian.cube.openshift.impl.client.CubeOpenShiftConfiguration;
import org.arquillian.cube.openshift.impl.utils.StringResolver;
import org.arquillian.cube.openshift.impl.utils.Strings;
import org.arquillian.cube.openshift.impl.utils.TemplateUtils;
import org.jboss.arquillian.test.spi.TestClass;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 * @author <a href="mailto:mluksa@redhat.com">Marko Luksa</a>
 */
public class OpenShiftResourceFactory {
    private static final Logger log = Logger.getLogger(OpenShiftResourceFactory.class.getName());

    public static final String CLASSPATH_PREFIX = "classpath:";
    public static final String ARCHIVE_PREFIX = "archive:";
    public static final String URL_PREFIX = "http";

    private static final OSRFinder OSR_FINDER = new OSRFinder();
    private static final RBFinder RB_FINDER = new RBFinder();
    private static final ARSAFinder ARSA_FINDER = new ARSAFinder();
    private static final TEMPFinder TEMP_FINDER = new TEMPFinder();

    public static void createResources(String resourcesKey, OpenShiftAdapter adapter, Class<?> testClass, Properties properties) {
        try {
            final StringResolver resolver = Strings.createStringResolver(properties);

            List<OpenShiftResource> openShiftResources = new ArrayList<>();
            OSR_FINDER.findAnnotations(openShiftResources, testClass);
            createOpenShiftResources(resourcesKey, adapter, testClass, resolver, openShiftResources);

            List<RoleBinding> roleBindings = new ArrayList<>();
            RB_FINDER.findAnnotations(roleBindings, testClass);
            createRoleBindings(resourcesKey, adapter, resolver, roleBindings);

            List<AddRoleToServiceAccount> arsaBindings = new ArrayList<>();
            ARSA_FINDER.findAnnotations(arsaBindings, testClass);
            createRolesToServiceAccounts(resourcesKey, adapter, resolver, arsaBindings);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static void createResources(Class<?> testClass, OpenShiftAdapter client, Method testMethod, Properties properties) {
        final StringResolver resolver = Strings.createStringResolver(properties);

        final List<OpenShiftResource> openShiftResources = Arrays.asList(testMethod.getAnnotationsByType(OpenShiftResource.class));
        try {
            createOpenShiftResources(createResourceKey(testClass, testMethod), client, testClass, resolver, openShiftResources);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static void createRolesToServiceAccounts(String resourcesKey, OpenShiftAdapter adapter,
        StringResolver resolver, List<AddRoleToServiceAccount> arsaBindings) {
        for (AddRoleToServiceAccount arsa : arsaBindings) {
            String role = resolver.resolve(arsa.role());
            String saPattern = String.format("system:serviceaccount:${kubernetes.namespace}:%s", arsa.serviceAccount());
            String serviceAccount = resolver.resolve(saPattern);
            log.info(String.format("Adding role %s to service account %s", role, serviceAccount));
            adapter.addRoleBinding(resourcesKey, role, serviceAccount);
        }
    }

    private static void createRoleBindings(String resourcesKey, OpenShiftAdapter adapter, StringResolver resolver,
        List<RoleBinding> roleBindings) {
        for (RoleBinding rb : roleBindings) {
            String roleRefName = resolver.resolve(rb.roleRefName());
            String userName = resolver.resolve(rb.userName());
            log.info(String.format("Adding new role binding: %s / %s", roleRefName, userName));
            adapter.addRoleBinding(resourcesKey, roleRefName, userName);
        }
    }

    private static void createOpenShiftResources(String resourcesKey, OpenShiftAdapter adapter, Class<?> testClass,
        StringResolver resolver, List<OpenShiftResource> openShiftResources) throws IOException {
        for (OpenShiftResource osr : openShiftResources) {
            String file = resolver.resolve(osr.value());

            InputStream stream;
            if (file.startsWith(URL_PREFIX)) {
                stream = new URL(file).openStream();
            } else if (file.startsWith(CLASSPATH_PREFIX)) {
                String resourceName = file.substring(CLASSPATH_PREFIX.length());
                stream = testClass.getClassLoader().getResourceAsStream(resourceName);
                if (stream == null) {
                    throw new IllegalArgumentException("Could not find resource on classpath: " + resourceName);
                }
            } else {
                stream = new ByteArrayInputStream(file.getBytes());
            }

            log.info(String.format("Creating new OpenShift resource: %s", file));
            adapter.createResource(resourcesKey, stream);
        }
    }

    /**
     * Aggregates a list of templates specified by @Template
     */
    static <T> List<Template> getTemplates(T objectType) {
        try {
            List<Template> templates = new ArrayList<>();
            TEMP_FINDER.findAnnotations(templates, objectType);
            return templates;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns true if templates are to be instantiated synchronously and false if
     * asynchronously.
     */
    static <T> boolean syncInstantiation(T objectType) {
        List<Template> templates = new ArrayList<>();
        Templates tr = TEMP_FINDER.findAnnotations(templates, objectType);
        if (tr == null) {
        	/* Default to synchronous instantiation */
        	return true;
        } else {
            return tr.syncInstantiation();
        }
    }

    private static void deleteResources(String resourcesKey, OpenShiftAdapter adapter) {
        try {
            adapter.deleteResources(resourcesKey);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static void deleteTemplates(final String templateKeyPrefix, Class<?> testClass, List<Template> templates,
        OpenShiftAdapter openshiftAdapter, CubeOpenShiftConfiguration configuration)
        throws Exception {
        StringResolver resolver;
        String templateURL;
        for (Template template : templates) {
            // Delete pods and services related to each template
            resolver = Strings.createStringResolver(configuration.getProperties());
            templateURL = TemplateUtils.readTemplateUrl(template, testClass, configuration, false, resolver);

            openshiftAdapter.deleteTemplate(templateKeyPrefix + templateURL);
        }
    }

    public static void deleteResources(Class<?> testClass, Method testMethod, OpenShiftAdapter client) {
        try {
            client.deleteResources(createResourceKey(testClass, testMethod));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static void deleteEnvironment(final TestClass testClass, OpenShiftAdapter client,
        CubeOpenShiftConfiguration configuration, List<Template> templates)
        throws Exception {
        if (configuration.getCubeConfiguration().isNamespaceCleanupEnabled()) {
            final Class<?> javaClass = testClass.getJavaClass();
            log.info(String.format("Deleting environment for %s", testClass.getName()));
            deleteTemplates(testClass.getName(), javaClass, templates, client, configuration);
            deleteResources(testClass.getName(), client);
            additionalCleanup(client,
                Collections.singletonMap("test-case", javaClass.getSimpleName().toLowerCase()));
        } else {
            log.info(String.format("Ignoring cleanup for %s", testClass.getName()));
        }
    }

    static void additionalCleanup(OpenShiftAdapter client, Map<String, String> labels) throws Exception {
        client.cleanRemnants(labels);
    }

    public static String createResourceKey(Class<?> testClass, Method testMethod) {
        return testClass.getName() + "_" + testMethod.getName();
    }

    private static abstract class Finder<U extends Annotation, V extends Annotation> {

        protected abstract Class<U> getWrapperType();

        protected abstract Class<V> getSingleType();

        protected abstract V[] toSingles(U u);

        <T> U findAnnotations(List<V> annotations, T type) {
            if (type == Object.class) {
                return null;
            }
            if (type instanceof Class) {
                final Class<?> testClass = (Class<?>) type;
                U anns = testClass.getAnnotation(getWrapperType());
                addAnnotationsFromWrapper(anns, annotations);

                V ann = testClass.getAnnotation(getSingleType());
                if (ann != null) {
                    annotations.add(0, ann);
                }

                findAnnotations(annotations, testClass.getSuperclass());
                return anns;
            } else if (type instanceof Method) {
                final Method testMethod = (Method) type;
                U anns = testMethod.getAnnotation(getWrapperType());
                addAnnotationsFromWrapper(anns, annotations);

                V ann = testMethod.getAnnotation(getSingleType());
                if (ann != null) {
                    annotations.add(0, ann);
                }
                return anns;
            }
            return null;
        }

        void addAnnotationsFromWrapper(U anns, List<V> annotations) {
            if (anns != null) {
                V[] ann = toSingles(anns);
                for (int i = ann.length - 1; i >= 0; i--) {
                    annotations.add(0, ann[i]);
                }
            }
        }

    }

    private static class OSRFinder extends Finder<OpenShiftResource.List, OpenShiftResource> {
        protected Class<OpenShiftResource.List> getWrapperType() {
            return OpenShiftResource.List.class;
        }

        protected Class<OpenShiftResource> getSingleType() {
            return OpenShiftResource.class;
        }

        protected OpenShiftResource[] toSingles(OpenShiftResource.List openShiftResources) {
            return openShiftResources.value();
        }
    }

    private static class RBFinder extends Finder<RoleBinding.List, RoleBinding> {
        protected Class<RoleBinding.List> getWrapperType() {
            return RoleBinding.List.class;
        }

        protected Class<RoleBinding> getSingleType() {
            return RoleBinding.class;
        }

        protected RoleBinding[] toSingles(RoleBinding.List roleBindings) {
            return roleBindings.value();
        }
    }

    private static class ARSAFinder extends Finder<AddRoleToServiceAccount.List, AddRoleToServiceAccount> {
        protected Class<AddRoleToServiceAccount.List> getWrapperType() {
            return AddRoleToServiceAccount.List.class;
        }

        protected Class<AddRoleToServiceAccount> getSingleType() {
            return AddRoleToServiceAccount.class;
        }

        protected AddRoleToServiceAccount[] toSingles(AddRoleToServiceAccount.List roleBindings) {
            return roleBindings.value();
        }
    }

    private static class TEMPFinder extends Finder<Templates, Template> {
        protected Class<Templates> getWrapperType() {
            return Templates.class;
        }

        protected Class<Template> getSingleType() {
            return Template.class;
        }

        protected Template[] toSingles(Templates templates) {
            return templates.templates();
        }

        protected boolean syncInstantiation(Templates templates) {
            return templates.syncInstantiation();
        }
    }
}
