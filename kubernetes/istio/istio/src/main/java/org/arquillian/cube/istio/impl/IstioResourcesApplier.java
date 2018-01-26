package org.arquillian.cube.istio.impl;

import io.fabric8.kubernetes.api.model.v3_1.HasMetadata;
import io.fabric8.kubernetes.clnt.v3_1.KubernetesClient;
import io.fabric8.kubernetes.clnt.v3_1.KubernetesClientTimeoutException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import me.snowdrop.istio.client.IstioClient;
import org.arquillian.cube.istio.api.IstioResource;
import org.arquillian.cube.istio.api.IstioResources;
import org.arquillian.cube.kubernetes.impl.utils.RunnerExpressionParser;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.arquillian.test.spi.event.suite.AfterClass;
import org.jboss.arquillian.test.spi.event.suite.BeforeClass;

public class IstioResourcesApplier {

    private static final Logger log = Logger.getLogger(IstioResourcesApplier.class.getName());

    private List<HasMetadata> createdIstioResources = new ArrayList<>();

    public void applyIstioResouces(@Observes(precedence = -20) BeforeClass beforeClass, final IstioClient istioClient) {

        final TestClass testClass = beforeClass.getTestClass();

        Arrays.stream(findAnnotations(testClass))
            .map(IstioResource::value)
            .map(RunnerExpressionParser::parseExpressions)
            .map(IstioResourceResolver::resolve)
            .forEach(istioResource -> {
                try (InputStream istioResourceStream = istioResource.openStream() ) {
                    createdIstioResources.addAll(istioClient.registerCustomResources(istioResourceStream));
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });

    }

    public void removeIstioResouces(@Observes(precedence = 20) AfterClass beforeClass, final IstioClient istioClient) {
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
            if (testClass.isAnnotationPresent(IstioResources.class)) {
                return testClass.getAnnotation(IstioResources.class).value();
            }
        }

        return new IstioResource[0];
    }

}