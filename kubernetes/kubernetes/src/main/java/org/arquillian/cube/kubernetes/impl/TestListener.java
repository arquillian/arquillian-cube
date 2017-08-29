/**
 * Copyright 2005-2016 Red Hat, Inc.
 * <p>
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.arquillian.cube.kubernetes.impl;

import java.util.HashMap;
import java.util.Map;
import org.arquillian.cube.impl.util.Strings;
import org.arquillian.cube.kubernetes.api.AnnotationProvider;
import org.arquillian.cube.kubernetes.api.NamespaceService;
import org.arquillian.cube.kubernetes.api.Session;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.TestResult;
import org.jboss.arquillian.test.spi.event.suite.After;
import org.jboss.arquillian.test.spi.event.suite.Before;
import org.jboss.arquillian.test.spi.event.suite.TestLifecycleEvent;

import static org.arquillian.cube.kubernetes.api.AnnotationProvider.MAX_ANNOTATION_KEY_LENGTH;
import static org.arquillian.cube.kubernetes.api.AnnotationProvider.TEST_CASE_STATUS_FORMAT;

public class TestListener {

    @Inject
    Instance<DefaultSession> session;

    @Inject
    Instance<NamespaceService> namespaceService;

    static String getPackage(TestLifecycleEvent event) {
        if (event.getTestClass() == null) {
            return "";
        } else if (event.getTestClass().getJavaClass() == null) {
            return "";
        } else if (event.getTestClass().getJavaClass().getPackage() == null) {
            return "";
        } else {
            return event.getTestClass().getJavaClass().getPackage().getName();
        }
    }

    static String getClassName(TestLifecycleEvent event) {
        if (event.getTestClass() == null) {
            throw new IllegalArgumentException("TestLifecycleEvent does not have a valid test class");
        } else if (event.getTestClass().getJavaClass() == null) {
            throw new IllegalArgumentException("TestLifecycleEvent does not have a valid test class");
        } else {
            return event.getTestClass().getJavaClass().getName();
        }
    }

    static String getMethodName(TestLifecycleEvent event) {
        if (event.getTestMethod() == null) {
            throw new IllegalArgumentException("TestLifecycleEvent does not have a valid method name");
        } else {
            return event.getTestMethod().getName();
        }
    }

    static String trimName(String packageName, String className, String methodName) {
        StringBuilder sb = new StringBuilder();
        String trimmedPackage = trimPackage(packageName);
        if (Strings.isNotNullOrEmpty(packageName)) {
            sb.append(trimmedPackage).append(".");
        }
        sb.append(className).append(".").append(methodName);
        String result = sb.toString();
        int prefixLength = AnnotationProvider.TEST_CASE_STATUS_FORMAT.length() - 1;
        if (prefixLength + result.length() > MAX_ANNOTATION_KEY_LENGTH) {
            result = result.substring(prefixLength + result.length() - MAX_ANNOTATION_KEY_LENGTH);
        }
        if (result.charAt(0) == '.') {
            result = result.substring(1);
        }
        return result;
    }

    static String trimPackage(String pkg) {
        if (Strings.isNullOrEmpty(pkg)) {
            return pkg;
        }

        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String part : pkg.split("\\.")) {
            if (first) {
                first = false;
            } else {
                sb.append(".");
            }
            sb.append(part.substring(0, 1));
        }
        return sb.toString();
    }

    public void start(@Observes(precedence = Integer.MIN_VALUE) Before event) {
        Session session = this.session.get();
        NamespaceService namespaceService = this.namespaceService.get();

        String pkg = getPackage(event);
        String className = getClassName(event);
        String methodName = getMethodName(event);

        session.setCurrentMethodName(methodName);

        Map<String, String> annotations = new HashMap<>();
        String testCase = trimName(pkg, className, methodName);
        annotations.put(String.format(TEST_CASE_STATUS_FORMAT, testCase), Constants.RUNNING_STATUS);
        try {
            namespaceService.annotate(session.getNamespace(), annotations);
        } catch (Throwable t) {
            session.getLogger().warn("Could not annotate namespace:[" + session.getNamespace() +
                "] with test: [" + className + "] method: [" + methodName + "] state:[" + Constants.RUNNING_STATUS + "]");
        }
    }

    public void stop(@Observes(precedence = Integer.MIN_VALUE) After event, TestResult result) {
        Session session = this.session.get();
        NamespaceService namespaceService = this.namespaceService.get();

        String pkg = getPackage(event);
        String className = getClassName(event);
        String methodName = getMethodName(event);

        session.setCurrentMethodName(null);

        Map<String, String> annotations = new HashMap<>();
        String testCase = trimName(pkg, className, methodName);
        annotations.put(String.format(TEST_CASE_STATUS_FORMAT, testCase), result.getStatus().name());
        try {
            namespaceService.annotate(session.getNamespace(), annotations);
        } catch (Throwable t) {
            session.getLogger().warn("Could not annotate namespace:["
                + session.getNamespace()
                +
                "] with test: ["
                + className
                + "] method: ["
                + methodName
                + "] result:["
                + result.getStatus().name()
                + "]");
        }

        switch (result.getStatus()) {
            case PASSED:
                session.getPassed().incrementAndGet();
                break;
            case FAILED:
                session.getFailed().incrementAndGet();
                break;
            case SKIPPED:
                session.getSkipped().incrementAndGet();
        }
    }
}
