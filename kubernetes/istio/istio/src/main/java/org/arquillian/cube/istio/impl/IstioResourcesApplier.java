package org.arquillian.cube.istio.impl;

import io.fabric8.kubernetes.api.model.v4_0.HasMetadata;
import me.snowdrop.istio.client.IstioClient;
import org.arquillian.cube.istio.api.IstioResource;
import org.arquillian.cube.kubernetes.impl.resolver.ResourceResolver;
import org.arquillian.cube.kubernetes.impl.utils.RunnerExpressionParser;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.arquillian.test.spi.event.suite.AfterClass;
import org.jboss.arquillian.test.spi.event.suite.BeforeClass;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IstioResourcesApplier {

    private List<HasMetadata> createdIstioResources = new ArrayList<>();

    public void applyIstioResources(@Observes(precedence = -20) BeforeClass beforeClass, final IstioClient istioClient) {

        final TestClass testClass = beforeClass.getTestClass();

        Arrays.stream(findAnnotations(testClass))
            .map(IstioResource::value)
            .map(RunnerExpressionParser::parseExpressions)
            .map(ResourceResolver::resolve)
            .forEach(istioResource -> {
                try (BufferedInputStream istioResourceStream = new BufferedInputStream(istioResource.openStream()) ) {
                    createdIstioResources.addAll(istioClient.registerCustomResources(istioResourceStream));
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });

    }

    public void removeIstioResources(@Observes(precedence = 20) AfterClass beforeClass, final IstioClient istioClient) {
        try {
            for (HasMetadata istioResource : createdIstioResources) {
                istioClient.unregisterCustomResource((me.snowdrop.istio.api.model.IstioResource) istioResource);
            }
        } finally {
            createdIstioResources.clear();
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

}
