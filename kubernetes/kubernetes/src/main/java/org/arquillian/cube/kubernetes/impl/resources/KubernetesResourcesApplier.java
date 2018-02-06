package org.arquillian.cube.kubernetes.impl.resources;

import io.fabric8.kubernetes.api.model.v3_1.HasMetadata;
import io.fabric8.kubernetes.clnt.v3_1.KubernetesClient;
import org.arquillian.cube.kubernetes.annotations.KubernetesResource;
import org.arquillian.cube.kubernetes.impl.utils.RunnerExpressionParser;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.arquillian.test.spi.event.suite.After;
import org.jboss.arquillian.test.spi.event.suite.AfterClass;
import org.jboss.arquillian.test.spi.event.suite.Before;
import org.jboss.arquillian.test.spi.event.suite.BeforeClass;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class KubernetesResourcesApplier {

    private List<HasMetadata> createdKubernetesResources = new ArrayList<>();

    private Logger log = Logger.getLogger(KubernetesResourcesApplier.class.getName());

    public void applyKubernetesResourcesAtClassScope(@Observes(precedence = 10) BeforeClass beforeClass, final KubernetesClient kubernetesClient) {
        final TestClass testClass = beforeClass.getTestClass();
        final KubernetesResource[] kubernetesResources = findAnnotations(testClass);

        log.info(String.format("Creating environment for %s", testClass.getName()));

        createResources(kubernetesClient, kubernetesResources);
    }

    public void applyKubernetesResourcesAtMethodScope(@Observes(precedence = 10) Before beforeMethod, final KubernetesClient kubernetesClient) {
        final TestClass testClass = beforeMethod.getTestClass();
        final Method testMethod = beforeMethod.getTestMethod();
        final KubernetesResource[] kubernetesResources = findAnnotations(testMethod);

        log.info(String.format("Creating environment for %s method %s", testClass.getName(), testMethod.getName()));

        createResources(kubernetesClient, kubernetesResources);
    }


    public void removeKubernetesResourcesAtClassScope(@Observes(precedence = -10) AfterClass afterClass, final KubernetesClient kubernetesClient) {
        final TestClass testClass = afterClass.getTestClass();

        log.info(String.format("Deleting environment for %s", testClass.getName()));

        deleteResources(kubernetesClient);
    }

    public void removeKubernetesResourcesAtMethodScope(@Observes(precedence = -10) After afterMethod, final KubernetesClient kubernetesClient) {
        final TestClass testClass = afterMethod.getTestClass();
        final Method testMethod = afterMethod.getTestMethod();

        log.info(String.format("Deleting environment for %s method %s", testClass.getName(), testMethod.getName()));

        deleteResources(kubernetesClient);
    }

    private void createResources(KubernetesClient kubernetesClient, KubernetesResource[] kubernetesResources) {
        Arrays.stream(kubernetesResources)
            .map(KubernetesResource::value)
            .map(RunnerExpressionParser::parseExpressions)
            .map(KubernetesResourceResolver::resolve)
            .forEach(kubernetesResource -> {
                try (BufferedInputStream kubernetesResourceStream = new BufferedInputStream(kubernetesResource)) {
                    kubernetesClient.load(kubernetesResourceStream).createOrReplace();
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });
    }

    private void deleteResources(KubernetesClient kubernetesClient) {
        try {
            for (HasMetadata kubernetesResource : createdKubernetesResources) {
                kubernetesClient.resourceList(kubernetesResource).delete();
            }
        } finally {
            createdKubernetesResources.clear();
        }
    }

    private <T> KubernetesResource[] findAnnotations(T type) {

        if (type == Object.class) {
            return null;
        }
        if (type instanceof TestClass) {
            final TestClass testClass = (TestClass) type;
            if (testClass.isAnnotationPresent(KubernetesResource.class)) {
                return new KubernetesResource[] {testClass.getAnnotation(KubernetesResource.class)};
            } else {
                if (testClass.isAnnotationPresent(KubernetesResource.List.class)) {
                    return testClass.getAnnotation(KubernetesResource.List.class).value();
                }
            }
            return new KubernetesResource[0];
        } else if (type instanceof Method) {
            final Method testMethod = (Method) type;
            if (testMethod.isAnnotationPresent(KubernetesResource.class)) {
                return new KubernetesResource[] {testMethod.getAnnotation(KubernetesResource.class)};
            } else {
                if (testMethod.isAnnotationPresent(KubernetesResource.List.class)) {
                    return testMethod.getAnnotation(KubernetesResource.List.class).value();
                }
            }
            return new KubernetesResource[0];
        }
        return null;
    }
}
