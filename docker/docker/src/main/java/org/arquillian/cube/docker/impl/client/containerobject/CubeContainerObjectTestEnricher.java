package org.arquillian.cube.docker.impl.client.containerobject;


import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.arquillian.cube.CubeController;
import org.arquillian.cube.containerobject.Cube;
import org.arquillian.cube.containerobject.CubeDockerFile;
import org.arquillian.cube.HostPort;
import org.arquillian.cube.containerobject.Environment;
import org.arquillian.cube.containerobject.Image;
import org.arquillian.cube.containerobject.Link;
import org.arquillian.cube.containerobject.Volume;
import org.arquillian.cube.docker.impl.await.PollingAwaitStrategy;
import org.arquillian.cube.docker.impl.client.config.Await;
import org.arquillian.cube.docker.impl.client.config.BuildImage;
import org.arquillian.cube.docker.impl.client.config.CubeContainer;
import org.arquillian.cube.docker.impl.client.config.PortBinding;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.docker.impl.model.DockerCube;
import org.arquillian.cube.docker.impl.util.ContainerObjectUtil;
import org.arquillian.cube.docker.impl.util.DockerFileUtil;
import org.arquillian.cube.impl.client.enricher.HostPortTestEnricher;
import org.arquillian.cube.impl.util.ReflectionUtil;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.metadata.HasPortBindings;
import org.arquillian.cube.spi.metadata.HasPortBindings.PortAddress;
import org.arquillian.cube.spi.metadata.IsContainerObject;
import org.jboss.arquillian.core.api.Injector;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.spi.ServiceLoader;
import org.jboss.arquillian.test.spi.TestEnricher;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;

public class CubeContainerObjectTestEnricher implements TestEnricher {

    private static final Logger logger = Logger.getLogger(CubeContainerObjectTestEnricher.class.getName());

    @Inject Instance<CubeRegistry> cubeRegistryInstance;
    @Inject Instance<ServiceLoader> serviceLoader;
    @Inject Instance<CubeController> cubeControllerInstance;
    @Inject Instance<DockerClientExecutor> dockerClientExecutorInstance;
    @Inject Instance<Injector> injectorInstance;

    @Override
    public void enrich(Object testCase) {
        enrichAndReturnLinks(testCase);
    }

    private Set<String> enrichAndReturnLinks(Object testCase) {
        List<Field> cubeFields = ReflectionUtil.getFieldsWithAnnotation(testCase.getClass(), Cube.class);
        Set<String> links = new HashSet<>();
        if (cubeFields.size() > 0) {
            for (Field cubeField : cubeFields) {
                try {
                    logger.fine(String.format("Creating Container Object for field %s", cubeField.getName()));
                    links.add(enrichField(testCase, cubeField));
                } catch (IllegalAccessException e) {
                    throw new IllegalArgumentException(e);
                } catch (IOException e) {
                    throw new IllegalArgumentException(e);
                } catch (InvocationTargetException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        }

        return links;
    }

    /**
     *
     * @param testCase
     * @param field
     * @return returns the name of the cube if this cube would be linked by its parent, or null
     * @throws IllegalAccessException
     * @throws IOException
     * @throws InvocationTargetException
     */
    private String enrichField(Object testCase, Field field) throws IllegalAccessException, IOException, InvocationTargetException {
        final Object cubeContainerObject = field.get(testCase);
        if (cubeContainerObject == null) {
            final Cube cubeAnnotation = field.getAnnotation(Cube.class);

            final Class<?> cubeContainerClazz = field.getType();

            //First we check if this ContainerObject is defining a @CubeDockerFile in static method
            final List<Method> methodsWithCubeDockerFile = ReflectionUtil.getMethodsWithAnnotation(cubeContainerClazz, CubeDockerFile.class);

            if (methodsWithCubeDockerFile.size() > 1 ) {
                throw new IllegalArgumentException(
                        String.format("More than one %s annotation found and only one was expected. Methods where %s was found are; %s", CubeDockerFile.class.getSimpleName(), CubeDockerFile.class.getSimpleName(), methodsWithCubeDockerFile));
            }

            // User has defined @CubeDockerfile and @Image
            if ((methodsWithCubeDockerFile.size() == 1 || cubeContainerClazz.isAnnotationPresent(CubeDockerFile.class)) && cubeContainerClazz.isAnnotationPresent(Image.class)) {
                throw new IllegalArgumentException(String.format("Container Object %s has defined %s annotation and %s annotation together.", cubeContainerClazz.getSimpleName(), Image.class.getSimpleName(), CubeDockerFile.class.getSimpleName()));
            }

            File output = null;
            boolean imageSet = false;
            CubeDockerFile cubeContainerClazzAnnotation = null;
            final String cubeName = getCubeName(cubeAnnotation, cubeContainerClazz);
            // @Dockerfile is defined as static method
            if(methodsWithCubeDockerFile.size() == 1) {
                Method annotatedMethodWithCubeDockerFile = methodsWithCubeDockerFile.get(0);
                cubeContainerClazzAnnotation = annotatedMethodWithCubeDockerFile.getAnnotation(CubeDockerFile.class);
                final Object archive = annotatedMethodWithCubeDockerFile.invoke(null, new Object[0]);
                if (archive instanceof Archive) {
                    Archive<?> genericArchive = (Archive<?>) archive;
                    output = createTemporalDirectoryForCopyingDockerfile(cubeContainerClazz, cubeName);
                    logger.finer(String.format("Created %s directory for storing contents of %s cube.", output, cubeName));
                    genericArchive.as(ExplodedExporter.class).exportExplodedInto(output);
                }

            } else {
                // @Dockerfile is defined at class level
                if (cubeContainerClazz.isAnnotationPresent(CubeDockerFile.class)) {
                    cubeContainerClazzAnnotation = cubeContainerClazz.getAnnotation(CubeDockerFile.class);

                    //Copy Dockerfile and all contains of the same directory in a known directory.
                    output = createTemporalDirectoryForCopyingDockerfile(cubeContainerClazz, cubeName);
                    logger.finer(String.format("Created %s directory for storing contents of %s cube.", output, cubeName));

                    DockerFileUtil.copyDockerfileDirectory(cubeContainerClazz, cubeContainerClazzAnnotation, output);
                } else {
                    // If there is no annotation
                    if (!cubeContainerClazz.isAnnotationPresent(Image.class)) {
                        throw new IllegalArgumentException(String.format("Test class %s has a ContainerObject %s that is not annotated with %s or %s annotation.", testCase.getClass().getName(), cubeContainerClazz.getName(), CubeDockerFile.class.getSimpleName(), Image.class.getSimpleName()));
                    }
                    // We have set the image
                    imageSet = true;
                }
            }

            //Creates ContainerObject
            final Object containerObjectInstance = ReflectionUtil.newInstance(cubeContainerClazz.getName(), new Class[0], new Class[0], cubeContainerClazz);
            enrichContainerObject(containerObjectInstance);
            field.set(testCase, containerObjectInstance);

            // Get all fields annotated with @Cube (means they are inner containers).
            // Then call recursively enrich method again.
            // To reuse the same logic we call the enrich method but instead of passing a testcase class, we pass the container object instance
            final Set<String> links = enrichAndReturnLinks(containerObjectInstance);

            //Starts the cube.
            // Since it is called after the enrichment they will be created in correct order

            //Creates Cube and Registers into the Cube Registry
            final String[] cubePortBinding = getPortBindings(cubeAnnotation, cubeContainerClazz);
            final int[] awaitPorts = getAwaitPorts(cubeAnnotation, cubeContainerClazz);
            final Environment[] environmentVariables = getEnvironmentAnnotations(field, cubeContainerClazz);
            final Volume[] volumeAnnotations = getVolumeAnnotations(field, cubeContainerClazz);
            org.arquillian.cube.spi.Cube<?> cube;
            if (imageSet) {
                cube = createCubeFromImage(cubeName, cubePortBinding, ArrayUtils.toObject(awaitPorts), links, cubeContainerClazz.getAnnotation(Image.class), environmentVariables, volumeAnnotations, output, testCase.getClass());
            } else {
                cube = createCubeFromDockerfile(cubeName, cubePortBinding, ArrayUtils.toObject(awaitPorts), links, cubeContainerClazzAnnotation, environmentVariables, volumeAnnotations, output, testCase.getClass());
            }

            logger.finer(String.format("Created Cube with name %s and configuration %s", cubeName, cube.configuration()));
            cubeRegistryInstance.get().addCube(cube);

            CubeController cubeController = cubeControllerInstance.get();
            cubeController.create(cubeName);
            cubeController.start(cubeName);

            // It is not a native Arquillian Enricher to avoid to be used wrongly in a none container object.
            // Since it is only has sense in case of container object that it is running one container in the scope.
            // Moreover it has no much sense to get this information in case of not using container object pattern.
            enrichHostPort(containerObjectInstance, cube);


            return link(field, cubeName);
        }
        return null;
    }

    private void enrichHostPort(Object containerObjectInstance, org.arquillian.cube.spi.Cube<?> cube) throws IllegalAccessException {
        final List<Field> fieldsWithHostPort = ReflectionUtil.getFieldsWithAnnotation(containerObjectInstance.getClass(), HostPort.class);
        if (fieldsWithHostPort.isEmpty()) {
            return;
        }

        final HasPortBindings portBindings = cube.getMetadata(HasPortBindings.class);
        if (portBindings == null) {
            throw new IllegalArgumentException(String.format("Container Object %s contains fields annotated with %s but no ports are exposed by the container", containerObjectInstance.getClass().getSimpleName(), HostPort.class.getSimpleName()));
        }

        for ( Field field : fieldsWithHostPort) {
            final HostPort hostPort = field.getAnnotation(HostPort.class);
            int hostPortValue = hostPort.value();
            if (hostPortValue > 0) {
                final PortAddress bindingForExposedPort = portBindings.getMappedAddress(hostPortValue);
                if (bindingForExposedPort != null) {
                    field.set(containerObjectInstance, bindingForExposedPort.getPort());
                } else {
                    throw new IllegalArgumentException(String.format("Container Object %s contains field %s annotated with %s but exposed port %s is not exposed on container object.", containerObjectInstance.getClass().getSimpleName(), field.getName(), HostPort.class.getSimpleName(), hostPortValue));
                }
            } else {
                throw new IllegalArgumentException(String.format("Container Object %s contains field %s annotated with %s but do not specify any exposed port", containerObjectInstance.getClass().getSimpleName(), field.getName(), HostPort.class.getSimpleName()));
            }
        }
    }

    private String link(Field field, String cubeName) {
        if (field.isAnnotationPresent(Link.class)) {
            return field.getAnnotation(Link.class).value();
        } else {
            return cubeName + ":" + cubeName;
        }
    }


    private Volume[] getVolumeAnnotations(Field field, Class<?> cubeContainerClass) {
        final List<Volume> volumes = new ArrayList<>();

        if (field.isAnnotationPresent(Volume.class)) {
            Collections.addAll(volumes, field.getAnnotationsByType(Volume.class));
        }

        volumes.addAll((Collection<? extends Volume>) ContainerObjectUtil.getAllAnnotations(cubeContainerClass, Volume.class));

        return volumes.toArray(new Volume[volumes.size()]);

    }

    private Environment[] getEnvironmentAnnotations(Field field, Class<?> cubeContainerClass) {
        final List<Environment> environments = new ArrayList<>();

        if (field.isAnnotationPresent(Environment.class)) {
            Collections.addAll(environments, field.getAnnotationsByType(Environment.class));
        }

        environments.addAll((Collection<? extends Environment>) ContainerObjectUtil.getAllAnnotations(cubeContainerClass, Environment.class));

        return environments.toArray(new Environment[environments.size()]);
    }


    private String getCubeName(Cube fieldAnnotation, Class<?> cubeContainerClass) {
        final String cubeName = fieldAnnotation.value();
        if (!Cube.DEFAULT_VALUE.equals(cubeName)) {
            // We have found a valid cube name
            return cubeName;
        } else {
            // Needs to check if container object or one of his parents contains a Cube
            final String value = ContainerObjectUtil.getTopCubeAttribute(cubeContainerClass, "value", Cube.class, Cube.DEFAULT_VALUE);
            if (value != null && !Cube.DEFAULT_VALUE.equals(value)) {
                //We got the cubeName in containerobject
                return value;
            } else {
                //No override so we need to use the default logic
                return cubeContainerClass.getSimpleName();
            }
        }
    }

    private String[] getPortBindings(Cube fieldAnnotation, Class<?> cubeContainerClass) {
        final String[] portBindings = fieldAnnotation.portBinding();
        if (!Arrays.equals(portBindings, Cube.DEFAULT_PORT_BINDING)) {
            //We found the port binding
            return portBindings;
        } else {
            final String[] portBinding = ContainerObjectUtil.getTopCubeAttribute(cubeContainerClass, "portBinding", Cube.class, Cube.DEFAULT_PORT_BINDING);
            if (portBinding != null && !Arrays.equals(portBinding, Cube.DEFAULT_PORT_BINDING)) {
                // Container Object or one of his parents has a Cube with portBinding definition.
                return portBinding;
            }
        }
        return Cube.DEFAULT_PORT_BINDING;
    }

    private int[] getAwaitPorts(Cube fieldAnnotation, Class<?> cubeContainerClass) {
        final int[] awaitPorts = fieldAnnotation.awaitPorts();

        if (!Arrays.equals(awaitPorts, Cube.DEFAULT_AWAIT_PORT_BINDING)) {
            // We found the await
            return awaitPorts;
        } else {
            final int[] awaitPort = ContainerObjectUtil.getTopCubeAttribute(cubeContainerClass, "awaitPorts", Cube.class, Cube.DEFAULT_AWAIT_PORT_BINDING);

            if (awaitPort != null && !Arrays.equals(awaitPort, Cube.DEFAULT_AWAIT_PORT_BINDING)) {
                // Container Object or one of his parents has a Cube with await definition
                return awaitPort;
            }
        }

        return Cube.DEFAULT_AWAIT_PORT_BINDING;
    }

    private void enrichContainerObject(Object containerObjectInstance) {
        final Collection<TestEnricher> testEnrichers = serviceLoader.get().all(TestEnricher.class);
        for (TestEnricher testEnricher : testEnrichers) {
            //To avoid recursive.
            if (testEnricher != this && ! (testEnricher instanceof HostPortTestEnricher)) {
                testEnricher.enrich(containerObjectInstance);
            }
        }
    }


    private org.arquillian.cube.spi.Cube<?> createCubeFromDockerfile(String cubeName, String[] portBinding, Integer[] awaitPorts, Set<String> links, CubeDockerFile cubeContainerClazzAnnotation, Environment[] environments, Volume[] volumeAnnotations, File dockerfileLocation, Class<?> testClass) {
        CubeContainer configuration = createConfigurationFromDockerfie(portBinding, awaitPorts, links, cubeContainerClazzAnnotation, dockerfileLocation, environments, volumeAnnotations);
        DockerCube newCube = new DockerCube(cubeName, configuration, dockerClientExecutorInstance.get());
        newCube.addMetadata(IsContainerObject.class, new IsContainerObject(testClass));
        injectorInstance.get().inject(newCube);
        return newCube;
    }

    private org.arquillian.cube.spi.Cube<?> createCubeFromImage(String cubeName, String[] portBinding, Integer[] awaitPorts, Set<String> links, Image image, Environment[] environment, Volume[] volumeAnnotations, File dockerfileLocation, Class<?> testClass) {
        final CubeContainer configuration = createConfigurationFromImage(portBinding, awaitPorts, links, image, dockerfileLocation, environment, volumeAnnotations);
        DockerCube newCube = new DockerCube(cubeName, configuration, dockerClientExecutorInstance.get());
        newCube.addMetadata(IsContainerObject.class, new IsContainerObject(testClass));
        injectorInstance.get().inject(newCube);
        return newCube;
    }

    private CubeContainer createConfigurationFromDockerfie(String[] portBinding, Integer[] awaitPorts, Set<String> links, CubeDockerFile cubeContainerClazzAnnotation, File dockerfileLocation, Environment[] environments, Volume[] volumeAnnotations) {
        CubeContainer configuration = new CubeContainer();

        List<PortBinding> bindings = new ArrayList<PortBinding>();
        for(String binding : portBinding) {
            bindings.add(PortBinding.valueOf(binding));
        }
        configuration.setPortBindings(bindings);

        if (links.size() > 0) {
            configuration.setLinks(org.arquillian.cube.docker.impl.client.config.Link.valuesOf(links));
        }

        if (environments != null ) {
            final List<String> collectEnvironments = Arrays.stream(environments)
                    .map(environment -> environment.key() + "=" + environment.value())
                    .collect(Collectors.toList());
            configuration.setEnv(collectEnvironments);
        }

        if (volumeAnnotations != null) {
            final List<String> collectVolumes = Arrays.stream(volumeAnnotations)
                    .map(volume -> volume.hostPath() + ":" + volume.containerPath() + ":rw")
                    .collect(Collectors.toList());
            configuration.setBinds(collectVolumes);
        }

        BuildImage dockerfileConfiguration = new BuildImage(
                dockerfileLocation.getAbsolutePath(),
                null,
                cubeContainerClazzAnnotation.nocache(),
                cubeContainerClazzAnnotation.remove());

        configuration.setBuildImage(dockerfileConfiguration);

        final Await await = createAwait(awaitPorts);
        configuration.setAwait(await);

        return configuration;
    }

    private Await createAwait(Integer[] awaitPorts) {
        final Await await = new Await();
        await.setPorts(Arrays.asList(awaitPorts));
        await.setStrategy(PollingAwaitStrategy.TAG);
        return await;
    }

    private CubeContainer createConfigurationFromImage(String[] portBinding, Integer[] awaitPorts, Set<String> links, Image image, File dockerfileLocation, Environment[] environments, Volume[] volumeAnnotations) {
        CubeContainer configuration = new CubeContainer();

        List<PortBinding> bindings = new ArrayList<PortBinding>();
        for(String binding : portBinding) {
            bindings.add(PortBinding.valueOf(binding));
        }
        configuration.setPortBindings(bindings);

        if (links.size() > 0) {
            configuration.setLinks(org.arquillian.cube.docker.impl.client.config.Link.valuesOf(links));
        }

        if (environments != null ) {
            final List<String> collectEnvironments = Arrays.stream(environments)
                    .map(environment -> environment.key() + "=" + environment.value())
                    .collect(Collectors.toList());
            configuration.setEnv(collectEnvironments);
        }

        if (volumeAnnotations != null) {
            final List<String> collectVolumes = Arrays.stream(volumeAnnotations)
                    .map(volume -> volume.hostPath() + ":" + volume.containerPath() + ":rw")
                    .collect(Collectors.toList());
            configuration.setBinds(collectVolumes);
        }

        configuration.setImage(org.arquillian.cube.docker.impl.client.config.Image.valueOf(image.value()));

        final Await await = createAwait(awaitPorts);
        configuration.setAwait(await);
        return configuration;
    }

    private File createTemporalDirectoryForCopyingDockerfile(Class<?> cubeContainerClazz, String id) throws IOException {
        File dir = File.createTempFile(cubeContainerClazz.getSimpleName(), id);
        dir.delete();
        if (!dir.mkdirs()) {
            throw new IllegalArgumentException("Temp Dir for storing Dockerfile contents could not be created.");
        }
        dir.deleteOnExit();
        return dir;
    }

    @Override
    public Object[] resolve(Method method) {
        return new Object[0];
    }
}
