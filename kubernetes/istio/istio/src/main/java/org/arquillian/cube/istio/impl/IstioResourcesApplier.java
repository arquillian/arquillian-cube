package org.arquillian.cube.istio.impl;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import me.snowdrop.istio.client.IstioClient;
import org.arquillian.cube.istio.api.IstioResource;
import org.arquillian.cube.kubernetes.impl.resolver.ResourceResolver;
import org.arquillian.cube.kubernetes.impl.resources.KubernetesResourcesApplier;
import org.arquillian.cube.kubernetes.impl.utils.RunnerExpressionParser;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.arquillian.test.spi.event.suite.After;
import org.jboss.arquillian.test.spi.event.suite.AfterClass;
import org.jboss.arquillian.test.spi.event.suite.Before;
import org.jboss.arquillian.test.spi.event.suite.BeforeClass;

public class IstioResourcesApplier {

    private static final Logger log = Logger.getLogger(KubernetesResourcesApplier.class.getName());

    private Map<String, List<me.snowdrop.istio.api.model.IstioResource>> resourcesMap = new ConcurrentHashMap<>();

    public void applyIstioResourcesAtClassScope(@Observes(precedence = -20) BeforeClass beforeClass, final IstioClient istioClient) {
        final TestClass testClass = beforeClass.getTestClass();

        log.info(String.format("Creating Istio resource for %s", testClass.getName()));

        createResources(createResourceKey(testClass), istioClient, findAnnotations(testClass));
    }

    public void applyIstioResourcesAtMethodScope(@Observes(precedence = -20) Before beforeMethod, final IstioClient istioClient) {
        final TestClass testClass = beforeMethod.getTestClass();
        final Method testMethod = beforeMethod.getTestMethod();

        log.info(String.format("Creating Istio resource for %s method %s", testClass.getName(), testMethod.getName()));

        createResources(createResourceKey(testMethod), istioClient, findAnnotations(testMethod));
    }

    public void removeIstioResourcesAtClassScope(@Observes(precedence = 20) AfterClass afterClass, final IstioClient istioClient) {
        final TestClass testClass = afterClass.getTestClass();

        log.info(String.format("Deleting Istio resource for %s", testClass.getName()));

        deleteResources(createResourceKey(testClass), istioClient);
    }

    public void removeIstioResourcesAtMethodScope(@Observes(precedence = 20) After afterMethod, final IstioClient istioClient) {
        final TestClass testClass = afterMethod.getTestClass();
        final Method testMethod = afterMethod.getTestMethod();

        log.info(String.format("Deleting Istio resource for %s method %s", testClass.getName(), testMethod.getName()));

        deleteResources(createResourceKey(testMethod), istioClient);
    }

    private String createResourceKey(TestClass testClass) {
        return testClass.getJavaClass().getName();
    }

    private String createResourceKey(Method testMethod) {
        return testMethod.getDeclaringClass().getName() + "_" + testMethod.getName();
    }

    private void createResources(String resourceKey, IstioClient istioClient,
        IstioResource[] annotations) {
        Arrays.stream(annotations)
            .map(IstioResource::value)
            .map(RunnerExpressionParser::parseExpressions)
            .map(ResourceResolver::resolve)
            .forEach(istioResource -> {
                try (BufferedInputStream istioResourceStream = new BufferedInputStream(istioResource.openStream()) ) {
                    resourcesMap.put(resourceKey, istioClient.registerCustomResources(istioResourceStream));
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });
    }

    private void deleteResources(String resourceKey, IstioClient istioClient) {
        if(!resourcesMap.containsKey(resourceKey)) {
            return;
        }
        try {
            resourcesMap.get(resourceKey).forEach(istioClient::unregisterCustomResource);
        } finally {
            resourcesMap.remove(resourceKey);
        }
    }

    private IstioResource[] findAnnotations(TestClass testClass) {
        if (testClass.isAnnotationPresent(IstioResource.class)) {
            return new IstioResource[] {testClass.getAnnotation(IstioResource.class)};
        } else {
            if (testClass.isAnnotationPresent(IstioResource.List.class)) {
                return testClass.getAnnotation(IstioResource.List.class).value();
            }
        }

        return new IstioResource[0];
    }

    private IstioResource[] findAnnotations(Method testMethod) {
        if (testMethod.isAnnotationPresent(IstioResource.class)) {
            return new IstioResource[] {testMethod.getAnnotation(IstioResource.class)};
        } else {
            if (testMethod.isAnnotationPresent(IstioResource.List.class)) {
                return testMethod.getAnnotation(IstioResource.List.class).value();
            }
        }
        return new IstioResource[0];
    }

}
