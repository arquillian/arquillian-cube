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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import org.arquillian.cube.openshift.api.AddRoleToServiceAccount;
import org.arquillian.cube.openshift.api.AddRolesToServiceAccounts;
import org.arquillian.cube.openshift.api.OpenShiftResource;
import org.arquillian.cube.openshift.api.OpenShiftResources;
import org.arquillian.cube.openshift.api.RoleBinding;
import org.arquillian.cube.openshift.api.RoleBindings;
import org.arquillian.cube.openshift.api.Template;
import org.arquillian.cube.openshift.api.Templates;
import org.arquillian.cube.openshift.impl.adapter.OpenShiftAdapter;
import org.arquillian.cube.openshift.impl.utils.StringResolver;
import org.arquillian.cube.openshift.impl.utils.Strings;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.Node;

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
    public static List<Template> getTemplates(Class<?> testClass) {
        try {
            List<Template> templates = new ArrayList<>();
            TEMP_FINDER.findAnnotations(templates, testClass);
            return templates;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns true if templates are to be instantiated synchronously and false if
     * asynchronously.
     */
    public static boolean syncInstantiation(Class<?> testClass) {
    	List<Template> templates = new ArrayList<>();
        Templates tr = TEMP_FINDER.findAnnotations(templates, testClass);
        if (tr == null) {
        	/* Default to synchronous instantiation */
        	return true;
        } else {
        	return tr.syncInstantiation();
        }
    }

    public static void deleteResources(String resourcesKey, OpenShiftAdapter adapter) {
        try {
            adapter.deleteResources(resourcesKey);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static void deleteResources(Class<?> testClass, Method testMethod, OpenShiftAdapter client) {
        try {
            client.deleteResources(createResourceKey(testClass, testMethod));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String createResourceKey(Class<?> testClass, Method testMethod) {
        return testClass.getName() + testMethod.getName();
    }

    private static abstract class Finder<U extends Annotation, V extends Annotation> {

        protected abstract Class<U> getWrapperType();

        protected abstract Class<V> getSingleType();

        protected abstract V[] toSingles(U u);

        U findAnnotations(List<V> annotations, Class<?> testClass) {
            if (testClass == Object.class) {
                return null;
            }

            U anns = testClass.getAnnotation(getWrapperType());
            if (anns != null) {
                V[] ann = toSingles(anns);
                for (int i = ann.length - 1; i >= 0; i--) {
                    annotations.add(0, ann[i]);
                }
            }

            V ann = testClass.getAnnotation(getSingleType());
            if (ann != null) {
                annotations.add(0, ann);
            }

            findAnnotations(annotations, testClass.getSuperclass());
	    return anns;
        }

    }

    private static class OSRFinder extends Finder<OpenShiftResources, OpenShiftResource> {
        protected Class<OpenShiftResources> getWrapperType() {
            return OpenShiftResources.class;
        }

        protected Class<OpenShiftResource> getSingleType() {
            return OpenShiftResource.class;
        }

        protected OpenShiftResource[] toSingles(OpenShiftResources openShiftResources) {
            return openShiftResources.value();
        }
    }

    private static class RBFinder extends Finder<RoleBindings, RoleBinding> {
        protected Class<RoleBindings> getWrapperType() {
            return RoleBindings.class;
        }

        protected Class<RoleBinding> getSingleType() {
            return RoleBinding.class;
        }

        protected RoleBinding[] toSingles(RoleBindings roleBindings) {
            return roleBindings.value();
        }
    }

    private static class ARSAFinder extends Finder<AddRolesToServiceAccounts, AddRoleToServiceAccount> {
        protected Class<AddRolesToServiceAccounts> getWrapperType() {
            return AddRolesToServiceAccounts.class;
        }

        protected Class<AddRoleToServiceAccount> getSingleType() {
            return AddRoleToServiceAccount.class;
        }

        protected AddRoleToServiceAccount[] toSingles(AddRolesToServiceAccounts roleBindings) {
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
