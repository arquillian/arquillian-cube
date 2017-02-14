package org.arquillian.cube.docker.impl.client.containerobject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.arquillian.cube.ContainerObjectConfiguration;
import org.arquillian.cube.ContainerObjectFactory;
import org.arquillian.cube.containerobject.Cube;
import org.arquillian.cube.containerobject.Environment;
import org.arquillian.cube.containerobject.Link;
import org.arquillian.cube.containerobject.Volume;
import org.arquillian.cube.docker.impl.await.PollingAwaitStrategy;
import org.arquillian.cube.docker.impl.client.config.Await;
import org.arquillian.cube.docker.impl.client.config.CubeContainer;
import org.arquillian.cube.docker.impl.client.config.PortBinding;
import org.arquillian.cube.impl.util.ReflectionUtil;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.spi.TestEnricher;

public class CubeContainerObjectTestEnricher implements TestEnricher {

    private static final Logger logger = Logger.getLogger(CubeContainerObjectTestEnricher.class.getName());

    @Inject Instance<ContainerObjectFactory> containerObjectFactoryInstance;

    @Override
    public void enrich(Object testCase) {
        List<Field> cubeFields = ReflectionUtil.getFieldsWithAnnotation(testCase.getClass(), Cube.class);
        if (cubeFields.size() > 0) {
            for (Field cubeField : cubeFields) {
                try {
                    logger.fine(String.format("Creating Container Object for field %s", cubeField.getName()));
                    enrichField(testCase, cubeField);
                } catch (IllegalAccessException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        }
    }

    private void enrichField(Object testCase, Field field) throws IllegalAccessException {
        final Object cubeContainerObject = field.get(testCase);
        if (cubeContainerObject == null) {

            final Class<?> cubeContainerClazz = field.getType();

            final ContainerObjectConfiguration configuration = extractConfigFrom(field);
            final Object containerObjectInstance = containerObjectFactoryInstance.get().createContainerObject(cubeContainerClazz, configuration, testCase);
            field.set(testCase, containerObjectInstance);
        }
    }

    @Override
    public Object[] resolve(Method method) {
        return new Object[0];
    }

    private static ContainerObjectConfiguration extractConfigFrom(Field field) {
        return new CubeContainerObjectConfiguration(extractCubeContainerFrom(field));
    }

    private static CubeContainer extractCubeContainerFrom(Field field) {
        final CubeContainer cubeContainer = new CubeContainer();

        final Cube cubeAnnotation = field.getAnnotation(Cube.class);
        if (cubeAnnotation == null) {
            throw new IllegalArgumentException(String.format("Field %s requires to be annotated with %s annotation", field.getName(), Cube.class.getSimpleName()));
        }

        // cubeName from Cube::value()
        final String cubeName = cubeAnnotation.value();
        if (cubeName != null && !Cube.DEFAULT_VALUE.equals(cubeName)) {
            cubeContainer.setContainerName(cubeName);
        }

        // port bindings from Cube::portBinding()
        final String[] portBindingsFromAnnotation = cubeAnnotation.portBinding();
        if (portBindingsFromAnnotation != null && !Arrays.equals(portBindingsFromAnnotation, Cube.DEFAULT_PORT_BINDING)) {
            final List<PortBinding> portBindings = Arrays.stream(portBindingsFromAnnotation)
                    .map(PortBinding::valueOf)
                    .collect(Collectors.toList());
            cubeContainer.setPortBindings(portBindings);
        }

        // await using PollingStrategy with Cube::awaitPorts()
        final int[] awaitPortsFromAnnotation = cubeAnnotation.awaitPorts();
        if (awaitPortsFromAnnotation != null && !Arrays.equals(awaitPortsFromAnnotation, Cube.DEFAULT_AWAIT_PORT_BINDING)) {
            final Await await = new Await();
            await.setStrategy(PollingAwaitStrategy.TAG);
            await.setPorts(Arrays.asList(ArrayUtils.toObject(awaitPortsFromAnnotation)));
            cubeContainer.setAwait(await);
        }

        // environment variables from Environment annotations
        final Environment[] environmentVariablesFromAnnotations = field.getAnnotationsByType(Environment.class);
        if (environmentVariablesFromAnnotations != null && environmentVariablesFromAnnotations.length > 0) {
            final List<String> environmentVariables = Arrays.stream(environmentVariablesFromAnnotations)
                    .map(environment -> environment.key() + "=" + environment.value())
                    .collect(Collectors.toList());
            cubeContainer.setEnv(environmentVariables);
        }

        // volumes from Volume annotations
        final Volume[] volumesFromAnnotations = field.getAnnotationsByType(Volume.class);
        if (volumesFromAnnotations != null && volumesFromAnnotations.length > 0) {
            final List<String> volumeBindings = Arrays.stream(volumesFromAnnotations)
                    .map(volume -> volume.hostPath() + ":" + volume.containerPath() + ":rw")
                    .collect(Collectors.toList());
            cubeContainer.setBinds(volumeBindings);
        }

        // links from link annotations
        final Link[] linksFromAnnotations = field.getAnnotationsByType(Link.class);
        if (linksFromAnnotations != null && linksFromAnnotations.length > 0) {
            final List<org.arquillian.cube.docker.impl.client.config.Link> links = Arrays.stream(linksFromAnnotations)
                    .map(Link::value)
                    .map(org.arquillian.cube.docker.impl.client.config.Link::valueOf)
                    .collect(Collectors.toList());
            cubeContainer.setLinks(links);
        }

        return cubeContainer;
    }
}
