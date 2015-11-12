package org.arquillian.cube.docker.impl.client.containerobject;


import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Logger;

import org.arquillian.cube.CubeController;
import org.arquillian.cube.containerobject.*;
import org.arquillian.cube.containerobject.Cube;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.docker.impl.model.DockerCube;
import org.arquillian.cube.docker.impl.util.ContainerObjectUtil;
import org.arquillian.cube.docker.impl.util.DockerFileUtil;
import org.arquillian.cube.impl.util.ReflectionUtil;
import org.arquillian.cube.spi.*;
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

            final String cubeName = getCubeName(cubeAnnotation, cubeContainerClazz);
            final String[] cubePortBinding = getPortBindings(cubeAnnotation, cubeContainerClazz);

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
            // @Dockerfile is defined as static method
            if(methodsWithCubeDockerFile.size() == 1) {
                Method annotatedMethodWithCubeDockerFile = methodsWithCubeDockerFile.get(0);
                cubeContainerClazzAnnotation = annotatedMethodWithCubeDockerFile.getAnnotation(CubeDockerFile.class);
                final Object archive = annotatedMethodWithCubeDockerFile.invoke(null, new Object[0]);
                if (archive instanceof Archive) {
                    Archive<?> genericArchive = (Archive) archive;
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

            org.arquillian.cube.spi.Cube cube;
            if (imageSet) {
                cube = createCubeFromImage(cubeName, cubePortBinding, links, cubeContainerClazz.getAnnotation(Image.class), output, testCase.getClass());
            } else {
                cube = createCubeFromDockerfile(cubeName, cubePortBinding, links, cubeContainerClazzAnnotation, output, testCase.getClass());
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

    private void enrichHostPort(Object containerObjectInstance, org.arquillian.cube.spi.Cube cube) throws IllegalAccessException {
        final List<Field> fieldsWithHostPort = ReflectionUtil.getFieldsWithAnnotation(containerObjectInstance.getClass(), HostPort.class);

        for ( Field field : fieldsWithHostPort) {
            final HostPort hostPort = field.getAnnotation(HostPort.class);
            int hostPortValue = hostPort.value();
            if (hostPortValue > 0) {
                final Binding.PortBinding bindingForExposedPort = cube.bindings().getBindingForExposedPort(hostPortValue);

                if (bindingForExposedPort != null) {
                    field.set(containerObjectInstance, bindingForExposedPort.getBindingPort());
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

    private void enrichContainerObject(Object containerObjectInstance) {
        final Collection<TestEnricher> testEnrichers = serviceLoader.get().all(TestEnricher.class);
        for (TestEnricher testEnricher : testEnrichers) {
            //To avoid recursive.
            if (testEnricher != this) {
                testEnricher.enrich(containerObjectInstance);
            }
        }
    }


    private org.arquillian.cube.spi.Cube createCubeFromDockerfile(String cubeName, String[] portBinding, Set<String> links, CubeDockerFile cubeContainerClazzAnnotation, File dockerfileLocation, Class<?> testClass) {
        final Map<String, Object> configuration = createConfigurationFromDockerfie(portBinding, links, cubeContainerClazzAnnotation, dockerfileLocation);
        BaseCube newCube = new DockerCube(cubeName, configuration, dockerClientExecutorInstance.get());
        newCube.addMetadata(new IsContainerObject(testClass));
        injectorInstance.get().inject(newCube);
        return newCube;
    }

    private org.arquillian.cube.spi.Cube createCubeFromImage(String cubeName, String[] portBinding, Set<String> links, Image image, File dockerfileLocation, Class<?> testClass) {
        final Map<String, Object> configuration = createConfigurationFromImage(portBinding, links, image, dockerfileLocation);
        BaseCube newCube = new DockerCube(cubeName, configuration, dockerClientExecutorInstance.get());
        newCube.addMetadata(new IsContainerObject(testClass));
        injectorInstance.get().inject(newCube);
        return newCube;
    }

    private Map<String, Object> createConfigurationFromDockerfie(String[] portBinding, Set<String> links, CubeDockerFile cubeContainerClazzAnnotation, File dockerfileLocation) {
        Map<String, Object> configuration = new HashMap<>();
        configuration.put(DockerClientExecutor.PORT_BINDINGS, Arrays.asList(portBinding));

        if (links.size() > 0) {
            configuration.put(DockerClientExecutor.LINKS, new HashSet<>(links));
        }

        Map<String, Object> dockerfileConfiguration = new HashMap<>();
        dockerfileConfiguration.put(DockerClientExecutor.DOCKERFILE_LOCATION, dockerfileLocation.getAbsolutePath());
        dockerfileConfiguration.put(DockerClientExecutor.REMOVE, cubeContainerClazzAnnotation.remove());
        dockerfileConfiguration.put(DockerClientExecutor.NO_CACHE, cubeContainerClazzAnnotation.nocache());

        configuration.put(DockerClientExecutor.BUILD_IMAGE, dockerfileConfiguration);
        return configuration;
    }

    private Map<String, Object> createConfigurationFromImage(String[] portBinding, Set<String> links, Image image, File dockerfileLocation) {
        Map<String, Object> configuration = new HashMap<>();
        configuration.put(DockerClientExecutor.PORT_BINDINGS, Arrays.asList(portBinding));

        if (links.size() > 0) {
            configuration.put(DockerClientExecutor.LINKS, new HashSet<>(links));
        }

        configuration.put(DockerClientExecutor.IMAGE, image.value());
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
