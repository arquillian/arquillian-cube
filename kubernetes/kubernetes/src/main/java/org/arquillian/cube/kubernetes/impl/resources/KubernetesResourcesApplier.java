package org.arquillian.cube.kubernetes.impl.resources;

import io.fabric8.kubernetes.api.model.v3_1.HasMetadata;
import io.fabric8.kubernetes.clnt.v3_1.KubernetesClient;
import org.arquillian.cube.kubernetes.annotations.KubernetesResource;
import org.arquillian.cube.kubernetes.impl.adapter.KubernetesAdapter;
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

public class KubernetesResourcesApplier {

    private List<HasMetadata> createdKubernetesResources = new ArrayList<>();

    public void applyKubernetesResources(@Observes(precedence = -20) BeforeClass beforeClass, final KubernetesClient kubernetesClient) {

        final TestClass testClass = beforeClass.getTestClass();

        Arrays.stream(findAnnotations(testClass))
            .map(KubernetesResource::value)
            .map(RunnerExpressionParser::parseExpressions)
            .map(KubernetesResourceResolver::resolve)
            .forEach(kubernetesResource -> {
                try (BufferedInputStream kubernetesResourceStream = new BufferedInputStream(kubernetesResource)) {
                    KubernetesAdapter kubernetesAdapter = new KubernetesAdapter(kubernetesClient);
                    kubernetesAdapter.createResource(kubernetesResourceStream);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });

    }

    public void removeKubernetesResources(@Observes(precedence = 20) AfterClass beforeClass, final KubernetesClient kubernetesClient) {
        try {
            for (HasMetadata kubernetesResource : createdKubernetesResources) {
                KubernetesAdapter kubernetesAdapter = new KubernetesAdapter(kubernetesClient);
                kubernetesAdapter.deleteResources(kubernetesResource);
            }
        } finally {
            createdKubernetesResources.clear();
        }
    }

    private KubernetesResource[] findAnnotations(TestClass testClass) {

        if (testClass.isAnnotationPresent(KubernetesResource.class)) {
            return new KubernetesResource[] {testClass.getAnnotation(KubernetesResource.class)};
        } else {
            if (testClass.isAnnotationPresent(KubernetesResource.List.class)) {
                return testClass.getAnnotation(KubernetesResource.List.class).value();
            }
        }

        return new KubernetesResource[0];
    }
}
