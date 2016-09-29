/**
 *  Copyright 2005-2016 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package org.arquillian.cube.kubernetes.impl;

import org.arquillian.cube.kubernetes.api.AnnotationProvider;
import org.arquillian.cube.kubernetes.api.NamespaceService;
import org.arquillian.cube.kubernetes.api.Session;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.TestResult;
import org.jboss.arquillian.test.spi.event.suite.AfterTestLifecycleEvent;
import org.jboss.arquillian.test.spi.event.suite.BeforeTestLifecycleEvent;

import java.util.HashMap;
import java.util.Map;


import static org.arquillian.cube.kubernetes.api.AnnotationProvider.TEST_CASE_STATUS_FORMAT;
import static org.arquillian.cube.kubernetes.api.AnnotationProvider.MAX_ANNOTATION_KEY_LENGTH;

public class TestListener {


    public void start(@Observes(precedence = Integer.MIN_VALUE) BeforeTestLifecycleEvent event, NamespaceService namespaceService, Session session) {
        String pkg = event.getTestClass().getJavaClass().getPackage().getName();
        String className = event.getTestClass().getJavaClass().getSimpleName();
        String methodName = event.getTestMethod().getName();

        Map<String, String> annotations = new HashMap<>();
        String testCase = trimName(pkg, className, methodName);
        annotations.put(String.format(TEST_CASE_STATUS_FORMAT, testCase), "RUNNING");
        namespaceService.annotate(session.getNamespace(), annotations);
    }

    public void stop(@Observes(precedence = Integer.MIN_VALUE) AfterTestLifecycleEvent event, TestResult result, NamespaceService namespaceService, Session session) {
        String pkg = event.getTestClass().getJavaClass().getPackage().getName();
        String className = event.getTestClass().getJavaClass().getSimpleName();
        String methodName = event.getTestMethod().getName();

        Map<String, String> annotations = new HashMap<>();
        String testCase = trimName(pkg, className, methodName);
        annotations.put(String.format(TEST_CASE_STATUS_FORMAT, testCase), result.getStatus().name());
        namespaceService.annotate(session.getNamespace(), annotations);

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

    static String trimName(String packageName, String className, String methodName) {
        StringBuilder sb = new StringBuilder();
        sb.append(trimPackage(packageName)).append(".").append(className).append(".").append(methodName);
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
}
