package org.arquillian.cube.docker.impl.client.containerobject.dsl;

import org.arquillian.cube.CubeController;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.docker.impl.model.DockerCube;
import org.arquillian.cube.docker.impl.model.NetworkRegistry;
import org.arquillian.cube.impl.util.ReflectionUtil;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.metadata.IsContainerObject;
import org.arquillian.cube.spi.metadata.IsNetworkContainerObject;
import org.jboss.arquillian.core.api.Injector;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.spi.TestEnricher;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

/**
 * Enricher that starts any networks or containers defined in tests. This enricher must start both because we need to be sure that networks are started before containers.
 */
public class ContainerNetworkObjectDslTestEnricher implements TestEnricher {

    private static final Logger logger = Logger.getLogger(ContainerNetworkObjectDslTestEnricher.class.getName());

    @Inject
    Instance<CubeController> cubeControllerInstance;

    @Inject
    Instance<CubeRegistry> cubeRegistryInstance;

    @Inject
    Instance<DockerClientExecutor> dockerClientExecutorInstance;

    @Inject
    Instance<NetworkRegistry> networkRegistryInstance;

    @Inject
    Instance<Injector> injectorInstance;

    @Override
    public void enrich(final Object testCase) {
        // Needs to get definitions of network and contaienrs at same enricher because first we need to register networks and then containers
        // And there is no way to order test enrichers.
        startNetworks(testCase);
        startContainers(testCase);

    }

    private void startContainers(Object testCase) {
        final List<Field> containerFields = ReflectionUtil.getFieldsWithAnnotation(testCase.getClass(), DockerContainer.class);
        Collections.sort(containerFields, Comparator.comparingInt(f -> f.getAnnotation(DockerContainer.class).order()));
        Collections.reverse(containerFields);

        for (Field field : containerFields) {
            try {
                final Object object = field.get(testCase);
                if (Container.class.isAssignableFrom(object.getClass())) {

                    Container containerObject = (Container) object;
                    final Container enrichedContainer = injectorInstance.get().inject(containerObject);
                    field.set(testCase, enrichedContainer);
                    startContainer(enrichedContainer, testCase.getClass());

                } else {
                    throw new IllegalArgumentException(String.format("Object %s is not assignable to %s.", object.getClass(), Container.class.getName()));
                }
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    private void startNetworks(Object testCase) {
        List<Field> networkFields = ReflectionUtil.getFieldsWithAnnotation(testCase.getClass(), DockerNetwork.class);

        for (Field field : networkFields) {
            try {

                final Object object = field.get(testCase);
                if (Network.class.isAssignableFrom(object.getClass())) {
                    Network networkObject = (Network) object;

                    startNetwork(networkObject);

                } else {
                    throw new IllegalArgumentException(String.format("Object %s is not assignable to %s.", object.getClass(), Network.class.getName()));
                }

            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    @Override
    public Object[] resolve(Method method) {
        return new Object[0];
    }

    private void startNetwork(Network network) {
        final org.arquillian.cube.docker.impl.client.config.Network dockerNetwork = network.getNetwork();
        final String id = dockerClientExecutorInstance.get().createNetwork(network.getId(), dockerNetwork);
        dockerNetwork.addMetadata(IsNetworkContainerObject.class, new IsNetworkContainerObject());
        networkRegistryInstance.get().addNetwork(id, dockerNetwork);
    }

    private void startContainer(Container container, Class<?> testClass) {
        String containerName = container.getContainerName();
        if (isNotInitialized(containerName)) {

            DockerCube dockerCube = new DockerCube(containerName, container.getCubeContainer(), dockerClientExecutorInstance.get());
            dockerCube.addMetadata(IsContainerObject.class, new IsContainerObject(testClass, container.getConnectionMode()));
            logger.finer(String.format("Created Cube with name %s and configuration %s", containerName, dockerCube.configuration()));
            cubeRegistryInstance.get().addCube(injectorInstance.get().inject(dockerCube));
            CubeController cubeController = cubeControllerInstance.get();
            cubeController.create(containerName);
            cubeController.start(containerName);
        }
    }

    private boolean isNotInitialized(String containerName) {
        return cubeRegistryInstance.get().getCube(containerName) == null;
    }
}
