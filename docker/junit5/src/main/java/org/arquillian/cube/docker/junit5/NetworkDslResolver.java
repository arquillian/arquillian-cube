package org.arquillian.cube.docker.junit5;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.arquillian.cube.docker.impl.client.containerobject.dsl.Network;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.docker.junit.DockerClientInitializer;
import org.arquillian.cube.docker.junit.Reflections;
import org.arquillian.cube.spi.metadata.IsNetworkContainerObject;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class NetworkDslResolver implements BeforeAllCallback, BeforeEachCallback, AfterAllCallback, AfterEachCallback {

    private final DockerClientExecutor dockerClientExecutor;

    private final List<String> networkPerClass = new ArrayList<>();
    private final List<String> networkPerMethod = new ArrayList<>();

    public NetworkDslResolver() {
        this.dockerClientExecutor = DockerClientInitializer.initialize();
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) {
        for (String networkId : networkPerClass) {
            dockerClientExecutor.removeNetwork(networkId);
        }
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) {
        for (String networkId : networkPerMethod) {
            dockerClientExecutor.removeNetwork(networkId);
        }
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {

        final Class<?> testClass = extensionContext.getRequiredTestClass();
        final Optional<Object> testInstance = extensionContext.getTestInstance();
        final List<Field> allStaticNetworkDslFields =
            Reflections.findAllFieldsOfType(testClass, NetworkDsl.class, f -> Modifier
                .isStatic(f.getModifiers()));

        networkPerClass.addAll(before(testInstance, allStaticNetworkDslFields));
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {

        final Class<?> testClass = extensionContext.getRequiredTestClass();
        final Optional<Object> testInstance = extensionContext.getTestInstance();
        final List<Field> allNetworkDslFields =
            Reflections.findAllFieldsOfType(testClass, NetworkDsl.class, f -> !Modifier
                .isStatic(f.getModifiers()));

        networkPerMethod.addAll(before(testInstance, allNetworkDslFields));

    }

    private List<String> before(Optional<Object> testInstanceOptional, List<Field> allStaticNetworkDslFields)
        throws IllegalAccessException {

        final List<String> networks = new ArrayList<>();

        final Object testInstance = testInstanceOptional.orElse(null);

        for (final Field networkDslField : allStaticNetworkDslFields) {
            NetworkDsl networkDsl = (NetworkDsl) networkDslField.get(testInstance);
            final Network network = networkDsl.buildNetwork();
            final org.arquillian.cube.docker.impl.client.config.Network dockerNetwork = network.getNetwork();

            final String networkId = dockerClientExecutor.createNetwork(network.getId(), dockerNetwork);
            dockerNetwork.addMetadata(IsNetworkContainerObject.class, new IsNetworkContainerObject());

            networks.add(networkId);
        }

        return networks;
    }

}
