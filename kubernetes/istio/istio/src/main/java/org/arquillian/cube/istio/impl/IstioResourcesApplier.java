package org.arquillian.cube.istio.impl;

import io.fabric8.kubernetes.api.model.v4_10.ObjectMeta;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import me.snowdrop.istio.client.IstioClient;
import org.arquillian.cube.istio.api.IstioResource;
import org.arquillian.cube.istio.api.RestoreIstioResource;
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

    private Map<String, List<me.snowdrop.istio.api.IstioResource>> resourcesMap = new ConcurrentHashMap<>();
    private Map<String, List<me.snowdrop.istio.api.IstioResource>> restoredResourcesMap = new ConcurrentHashMap<>();

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

        deleteResources(createResourceKey(testClass), istioClient, findRestoreAnnotations(testClass));
    }

    public void removeIstioResourcesAtMethodScope(@Observes(precedence = 20) After afterMethod, final IstioClient istioClient) {
        final TestClass testClass = afterMethod.getTestClass();
        final Method testMethod = afterMethod.getTestMethod();

        log.info(String.format("Deleting Istio resource for %s method %s", testClass.getName(), testMethod.getName()));

        deleteResources(createResourceKey(testMethod), istioClient, findRestoreAnnotations(testMethod));
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

    private void deleteResources(String resourceKey, IstioClient istioClient, RestoreIstioResource[] annotations) {

        // We apply the restore method first so the restored rules are populated.
        createRestoreResources(resourceKey, istioClient, annotations);

        if(!resourcesMap.containsKey(resourceKey)) {
            return;
        }
        try {
            final List<me.snowdrop.istio.api.IstioResource> istioResources = resourcesMap.get(resourceKey);

            for (me.snowdrop.istio.api.IstioResource resource : istioResources) {

                // If no restore or an Istio Resource has not been restored then we need to delete
                if(!restoredResourcesMap.containsKey(resourceKey) || !restored(resourceKey, resource.getMetadata())) {
                    istioClient.unregisterCustomResource(resource);
                }
            }

        } finally {
            resourcesMap.remove(resourceKey);
        }
    }

    private boolean restored(String resourceKey, ObjectMeta istioResourceToDelete) {
        final List<me.snowdrop.istio.api.IstioResource> listRestoredIstioResources = restoredResourcesMap.get(resourceKey);

        for (me.snowdrop.istio.api.IstioResource restoredIstioResources : listRestoredIstioResources) {
            final ObjectMeta restoredMetadata = restoredIstioResources.getMetadata();
            if (restoredMetadata.getName().equals(istioResourceToDelete.getName())
                && restoredMetadata.getNamespace().equals(istioResourceToDelete.getNamespace())) {
                return true;
            }
        }

        return false;

    }

    private void createRestoreResources(String resourceKey, IstioClient istioClient,
        RestoreIstioResource[] annotations) {
        Arrays.stream(annotations)
            .map(RestoreIstioResource::value)
            .map(RunnerExpressionParser::parseExpressions)
            .map(ResourceResolver::resolve)
            .forEach(istioResource -> {
                try (BufferedInputStream istioResourceStream = new BufferedInputStream(istioResource.openStream()) ) {
                    restoredResourcesMap.put(resourceKey, istioClient.registerCustomResources(istioResourceStream));
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });
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

    private RestoreIstioResource[] findRestoreAnnotations(TestClass testClass) {
        if (testClass.isAnnotationPresent(RestoreIstioResource.class)) {
            return new RestoreIstioResource[] {testClass.getAnnotation(RestoreIstioResource.class)};
        } else {
            if (testClass.isAnnotationPresent(RestoreIstioResource.List.class)) {
                return testClass.getAnnotation(RestoreIstioResource.List.class).value();
            }
        }

        return new RestoreIstioResource[0];
    }

    private RestoreIstioResource[] findRestoreAnnotations(Method testMethod) {
        if (testMethod.isAnnotationPresent(RestoreIstioResource.class)) {
            return new RestoreIstioResource[] {testMethod.getAnnotation(RestoreIstioResource.class)};
        } else {
            if (testMethod.isAnnotationPresent(RestoreIstioResource.List.class)) {
                return testMethod.getAnnotation(RestoreIstioResource.List.class).value();
            }
        }
        return new RestoreIstioResource[0];
    }

    public Map<String, List<me.snowdrop.istio.api.IstioResource>> getResourcesMap() {
        return Collections.unmodifiableMap(resourcesMap);
    }

    public Map<String, List<me.snowdrop.istio.api.IstioResource>> getRestoredResourcesMap() {
        return Collections.unmodifiableMap(restoredResourcesMap);
    }
}
