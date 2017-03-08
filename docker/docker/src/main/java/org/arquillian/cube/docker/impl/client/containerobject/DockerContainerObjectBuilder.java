package org.arquillian.cube.docker.impl.client.containerobject;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.arquillian.cube.ContainerObjectConfiguration;
import org.arquillian.cube.CubeController;
import org.arquillian.cube.CubeIp;
import org.arquillian.cube.HostPort;
import org.arquillian.cube.containerobject.Cube;
import org.arquillian.cube.containerobject.CubeDockerFile;
import org.arquillian.cube.containerobject.Environment;
import org.arquillian.cube.containerobject.Image;
import org.arquillian.cube.containerobject.Volume;
import org.arquillian.cube.docker.impl.await.PollingAwaitStrategy;
import org.arquillian.cube.docker.impl.client.config.Await;
import org.arquillian.cube.docker.impl.client.config.BuildImage;
import org.arquillian.cube.docker.impl.client.config.CubeContainer;
import org.arquillian.cube.docker.impl.client.config.Link;
import org.arquillian.cube.docker.impl.client.config.PortBinding;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.docker.impl.model.DockerCube;
import org.arquillian.cube.docker.impl.util.ContainerObjectUtil;
import org.arquillian.cube.docker.impl.util.DockerFileUtil;
import org.arquillian.cube.impl.client.enricher.CubeIpTestEnricher;
import org.arquillian.cube.impl.client.enricher.HostPortTestEnricher;
import org.arquillian.cube.impl.util.ReflectionUtil;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.metadata.HasPortBindings;
import org.arquillian.cube.spi.metadata.IsContainerObject;
import org.jboss.arquillian.test.spi.TestEnricher;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;

/**
 * Instantiate container objects. This class is not thread safe.
 *
 * @see DockerContainerObjectFactory
 *
 * @author <a href="mailto:rivasdiaz@gmail.com">Ramon Rivas</a>
 */
public class DockerContainerObjectBuilder<T> {

    private static final Logger logger = Logger.getLogger(DockerContainerObjectBuilder.class.getName());

    public static final String TEMPORARY_FOLDER_PREFIX = "arquilliancube_";
    public static final String TEMPORARY_FOLDER_SUFFIX = ".build";

    // parameters received
    private final DockerClientExecutor dockerClientExecutor;
    private final CubeController cubeController;
    private final CubeRegistry cubeRegistry;
    private Class<T> containerObjectClass;
    private Object containerObjectContainer;
    private CubeContainer providedConfiguration;
    private Collection<TestEnricher> enrichers = Collections.emptyList();
    private Consumer<DockerCube> cubeCreatedCallback;

    // temporary variables in the process of building the container and associated cube
    private boolean classHasMethodWithCubeDockerFile;
    private boolean classDefinesCubeDockerFile;
    private boolean classDefinesImage;
    private Method methodWithCubeDockerFile;
    private CubeDockerFile cubeDockerFileAnnotation;
    private Image cubeImageAnnotation;
    private String containerName;
    private File dockerfileLocation;
    private CubeContainer generatedConfigutation, mergedConfiguration;

    // results of the building
    private T containerObjectInstance;
    private DockerCube dockerCube;

    public DockerContainerObjectBuilder(DockerClientExecutor dockerClientExecutor, CubeController cubeController, CubeRegistry cubeRegistry) {
        this.dockerClientExecutor = dockerClientExecutor;
        this.cubeController = cubeController;
        this.cubeRegistry = cubeRegistry;
    }

    /**
     * Specifies an optional object that has a strong reference to the object being created. If set, a reference to it
     * is stored as part of the metadata of the container object. This object is expected to control the lifecycle of
     * the container object
     *
     * @param containerObjectContainer the container object's container
     * @return the current builder instance
     *
     * @see IsContainerObject
     */
    public DockerContainerObjectBuilder<T> withContainerObjectContainer(Object containerObjectContainer) {
        this.containerObjectContainer = containerObjectContainer;
        return this;
    }

    /**
     * Specifies the container object class to be instantiated
     *
     * @param containerObjectClass container object class to be instantiated
     * @return the current builder instance
     */
    public DockerContainerObjectBuilder<T> withContainerObjectClass(Class<T> containerObjectClass) {
        if (containerObjectClass == null) {
            throw new IllegalArgumentException("container object class cannot be null");
        }
        this.containerObjectClass = containerObjectClass;

        //First we check if this ContainerObject is defining a @CubeDockerFile in static method
        final List<Method> methodsWithCubeDockerFile = ReflectionUtil.getMethodsWithAnnotation(containerObjectClass, CubeDockerFile.class);

        if (methodsWithCubeDockerFile.size() > 1) {
            throw new IllegalArgumentException(
                    String.format("More than one %s annotation found and only one was expected. Methods where annotation was found are: %s", CubeDockerFile.class.getSimpleName(), methodsWithCubeDockerFile));
        }

        classHasMethodWithCubeDockerFile = !methodsWithCubeDockerFile.isEmpty();
        classDefinesCubeDockerFile = containerObjectClass.isAnnotationPresent(CubeDockerFile.class);
        classDefinesImage = containerObjectClass.isAnnotationPresent(Image.class);

        if (classHasMethodWithCubeDockerFile) {
            methodWithCubeDockerFile = methodsWithCubeDockerFile.get(0);
            boolean isMethodStatic = Modifier.isStatic(methodWithCubeDockerFile.getModifiers());
            boolean methodHasNoArguments = methodWithCubeDockerFile.getParameterCount() == 0;
            boolean methodReturnsAnArchive = Archive.class.isAssignableFrom(methodWithCubeDockerFile.getReturnType());
            if (!isMethodStatic || !methodHasNoArguments || !methodReturnsAnArchive) {
                throw new IllegalArgumentException(
                        String.format("Method %s annotated with %s is expected to be static, no args and return %s.", methodWithCubeDockerFile,  CubeDockerFile.class.getSimpleName(), Archive.class.getSimpleName()));
            }
        }

        // User has defined @CubeDockerfile on the class and a method
        if (classHasMethodWithCubeDockerFile && classDefinesCubeDockerFile) {
            throw new IllegalArgumentException(
                    String.format("More than one %s annotation found and only one was expected. Both class and method %s has the annotation.", CubeDockerFile.class.getSimpleName(), methodWithCubeDockerFile));
        }

        // User has defined @CubeDockerfile and @Image
        if ((classHasMethodWithCubeDockerFile || classDefinesCubeDockerFile) && classDefinesImage) {
            throw new IllegalArgumentException(
                    String.format("Container Object %s has defined %s annotation and %s annotation together.", containerObjectClass.getSimpleName(), Image.class.getSimpleName(), CubeDockerFile.class.getSimpleName()));
        }

        // User has not defined either @CubeDockerfile or @Image
        if (!classDefinesCubeDockerFile && !classDefinesImage && !classHasMethodWithCubeDockerFile) {
            throw new IllegalArgumentException(
                    String.format("Container Object %s is not annotated with either %s or %s annotations.", containerObjectClass.getName(), CubeDockerFile.class.getSimpleName(), Image.class.getSimpleName()));
        }

        return this;
    }

    /**
     * Specifies a configuration (can be partial) to be used to override the default configuration set using annotations
     * on the container object. The received configuration will be merged with the configuration extracted from the
     * container object, and the resulting configuration will be used to build the docker container.
     *
     * Currently only supports instances of {@link CubeContainerObjectConfiguration}
     *
     * @param configuration partial configuration to override default container object cube configuration
     * @return the current builder instance
     *
     * @see CubeContainerObjectConfiguration
     * @see CubeContainer
     */
    public DockerContainerObjectBuilder<T> withContainerObjectConfiguration(ContainerObjectConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("configuration cannot be null");
        }
        if (configuration != null && !(configuration instanceof CubeContainerObjectConfiguration)) {
            throw new IllegalArgumentException(
                    String.format("container object configuration received of type %s, but only %s is supported", configuration.getClass().getSimpleName(), CubeContainerObjectConfiguration.class.getSimpleName()));
        }
        this.providedConfiguration = configuration != null ? ((CubeContainerObjectConfiguration) configuration).getCubeContainerConfiguration() : null;

        return this;
    }

    /**
     * Specifies the list of enrichers that will be used to enrich the container object.
     *
     * @param enrichers list of enrichers that will be used to enrich the container object
     * @return the current builder instance
     */
    public DockerContainerObjectBuilder<T> withEnrichers(Collection<TestEnricher> enrichers) {
        if (enrichers == null) {
            throw new IllegalArgumentException("enrichers cannot be null");
        }
        this.enrichers = enrichers;
        return this;
    }

    /**
     * Specifies a consumer that will be executed after the cube object is created and after cube is created or started
     * by the cube controller. Callers must use this callback to register anything necesary for the controller to work
     * and also if they want to keep an instance of the created cube.
     *
     * @param cubeCreatedCallback consumer that will be called when the cube instance is created
     * @return the current builder instance
     */
    public DockerContainerObjectBuilder<T> onCubeCreated(Consumer<DockerCube> cubeCreatedCallback) {
        this.cubeCreatedCallback = cubeCreatedCallback;
        return this;
    }

    /**
     * Triggers the building process, builds, creates and starts the docker container associated with the requested
     * container object, creates the container object and returns it
     *
     * @return the created container object
     * @throws IllegalAccessException if there is an error accessing the container object fields
     * @throws IOException if there is an I/O error while preparing the docker build
     * @throws InvocationTargetException if there is an error while calling the DockerFile archive creation
     */
    public T build() throws IllegalAccessException, IOException, InvocationTargetException {
        generatedConfigutation = new CubeContainer();

        findContainerName();
        // if needed, prepare prepare resources required to build a docker image
        prepareImageBuild();
        // instantiate container object
        instantiateContainerObject();
        // enrich container object (without cube instance)
        enrichContainerObjectBeforeCube();
        // extract configuration from container object class
        extractConfigurationFromContainerObject();
        // merge received configuration with extracted configuration
        mergeContainerObjectConfiguration();
        // create/start/register associated cube
        initializeCube();
        // enrich container object (with cube instance)
        enrichContainerObjectWithCube();
        // return created container object
        return containerObjectInstance;
    }

    private void findContainerName() {
        // container name
        if (providedConfiguration != null) {
            final String providedContainerName = providedConfiguration.getContainerName();
            if (providedContainerName != null && !providedContainerName.isEmpty()) {
                containerName = providedConfiguration.getContainerName();
            }
        }
        if (containerName == null) {
            final String cubeValue = ContainerObjectUtil.getTopCubeAttribute(containerObjectClass, "value", Cube.class, Cube.DEFAULT_VALUE);
            if (cubeValue != null && !Cube.DEFAULT_VALUE.equals(cubeValue)) {
                containerName = cubeValue;
            }
        }
        if (containerName == null) {
            containerName = containerObjectClass.getSimpleName();
        }
        generatedConfigutation.setContainerName(containerName);
    }

    private void prepareImageBuild() throws InvocationTargetException, IllegalAccessException, IOException {

        // @CubeDockerfile is defined as static method
        if (classHasMethodWithCubeDockerFile) {
            cubeDockerFileAnnotation = methodWithCubeDockerFile.getAnnotation(CubeDockerFile.class);
            final Archive<?> archive = (Archive<?>) methodWithCubeDockerFile.invoke(null, new Object[0]);
            File output = createTemporalDirectoryForCopyingDockerfile(containerName);
            logger.finer(String.format("Created %s directory for storing contents of %s cube.", output, containerName));

            archive.as(ExplodedExporter.class).exportExplodedInto(output);
            dockerfileLocation = output;
        } else if (classDefinesCubeDockerFile) {
            cubeDockerFileAnnotation = containerObjectClass.getAnnotation(CubeDockerFile.class);

            //Copy Dockerfile and all contains of the same directory in a known directory.
            File output = createTemporalDirectoryForCopyingDockerfile(containerName);
            logger.finer(String.format("Created %s directory for storing contents of %s cube.", output, containerName));

            DockerFileUtil.copyDockerfileDirectory(containerObjectClass, cubeDockerFileAnnotation, output);
            dockerfileLocation = output;
        } else if (classDefinesImage) {
            cubeImageAnnotation = containerObjectClass.getAnnotation(Image.class);
        }
    }

    private void instantiateContainerObject() {
        containerObjectInstance = ReflectionUtil.newInstance(containerObjectClass.getName(), new Class[0], new Class[0], containerObjectClass);
    }

    private void enrichContainerObjectBeforeCube() {
        for (TestEnricher enricher: enrichers) {
            boolean requiresDockerInstanceCreated = enricher instanceof HostPortTestEnricher || enricher instanceof CubeIpTestEnricher;

            if (!requiresDockerInstanceCreated) {
                enricher.enrich(containerObjectInstance);
            }
        }
    }

    private void extractConfigurationFromContainerObject() {
        // this method will focus on extracting the configuration from the container object class
        // most probably, the caller will also try to pass some configuration from the current instantiation point
        // (for example, annotations on a field)
        // received configuration overrides container object configuration, so in some cases the extraction is skipped

        // port bindings
        if (providedConfiguration == null || providedConfiguration.getPortBindings() == null) {
            final String[] portBindingsFromAnnotation = ContainerObjectUtil.getTopCubeAttribute(containerObjectClass, "portBinding", Cube.class, Cube.DEFAULT_PORT_BINDING);

            if (portBindingsFromAnnotation != null && !Arrays.equals(portBindingsFromAnnotation, Cube.DEFAULT_PORT_BINDING)) {
                List<PortBinding> portBindings = Arrays.stream(portBindingsFromAnnotation)
                        .map(PortBinding::valueOf)
                        .collect(Collectors.toList());
                generatedConfigutation.setPortBindings(portBindings);
            }

        }
        // await
        if (providedConfiguration == null || providedConfiguration.getAwait() == null) {
            final int[] awaitPortsFromAnnotation = ContainerObjectUtil.getTopCubeAttribute(containerObjectClass, "awaitPorts", Cube.class, Cube.DEFAULT_AWAIT_PORT_BINDING);

            if (awaitPortsFromAnnotation != null && !Arrays.equals(awaitPortsFromAnnotation, Cube.DEFAULT_AWAIT_PORT_BINDING)) {
                final Await await = new Await();
                await.setStrategy(PollingAwaitStrategy.TAG);
                await.setPorts(Arrays.asList(ArrayUtils.toObject(awaitPortsFromAnnotation)));
                generatedConfigutation.setAwait(await);
            }
        }

        // environment variables
        // merged instead of overridden
        if (true) {
            List<String> environmentVariables = ContainerObjectUtil.getAllAnnotations(containerObjectClass, Environment.class)
                    .stream()
                    .map(environment -> environment.key() + "=" + environment.value())
                    .collect(Collectors.toList());
            generatedConfigutation.setEnv(environmentVariables);
        }

        // volumes
        // merged instead of overridden
        if (true) {
            List<String> volumeBindings = ContainerObjectUtil.getAllAnnotations(containerObjectClass, Volume.class)
                    .stream()
                    .map(volume -> volume.hostPath() + ":" + volume.containerPath() + ":rw")
                    .collect(Collectors.toList());
            generatedConfigutation.setBinds(volumeBindings);
        }

        // links
        if (providedConfiguration == null || providedConfiguration.getLinks() == null) {
            List<Link> links = ReflectionUtil.getFieldsWithAnnotation(containerObjectClass, Cube.class)
                   .stream()
                   .map(DockerContainerObjectBuilder::linkFromCubeAnnotatedField)
                   .collect(Collectors.toList());
            generatedConfigutation.setLinks(links);
        }

        // image
        if (classDefinesCubeDockerFile || classHasMethodWithCubeDockerFile) {
            BuildImage dockerfileConfiguration = new BuildImage(
                    dockerfileLocation.getAbsolutePath(),
                    null,
                    cubeDockerFileAnnotation.nocache(),
                    cubeDockerFileAnnotation.remove());
            generatedConfigutation.setBuildImage(dockerfileConfiguration);
        } else {
            generatedConfigutation.setImage(org.arquillian.cube.docker.impl.client.config.Image.valueOf(cubeImageAnnotation.value()));
        }
    }

    private void mergeContainerObjectConfiguration() {
        mergedConfiguration = new CubeContainer();
        if (providedConfiguration != null) {
            mergedConfiguration.merge(providedConfiguration);
        }
        mergedConfiguration.merge(generatedConfigutation);
        // TODO if both provided and generated configurations have environment variables or volumes, they must be merged instead.
        // TODO should this be handled in CubeContainer::merge() instead?
        if (providedConfiguration != null) {
            // environment variables
            if (providedConfiguration.getEnv() != null && generatedConfigutation.getEnv() != null) {
                Collection<String> env = new ArrayList<>();
                env.addAll(mergedConfiguration.getEnv());
                env.addAll(generatedConfigutation.getEnv());
                mergedConfiguration.setEnv(env);
            }
            // volumes
            if (providedConfiguration.getBinds() != null && generatedConfigutation.getBinds() != null) {
                Collection<String> binds = new ArrayList<>();
                binds.addAll(mergedConfiguration.getBinds());
                binds.addAll(generatedConfigutation.getBinds());
                mergedConfiguration.setBinds(binds);
            }
        }
    }

    private void initializeCube() {

        if (isNotInitialized()) {

            dockerCube = new DockerCube(containerName, mergedConfiguration, dockerClientExecutor);
            Class<?> containerObjectContainerClass = containerObjectContainer != null ? containerObjectContainer.getClass() : null;
            dockerCube.addMetadata(IsContainerObject.class, new IsContainerObject(containerObjectContainerClass));
            logger.finer(String.format("Created Cube with name %s and configuration %s", containerName, dockerCube.configuration()));
            if (cubeCreatedCallback != null) {
                cubeCreatedCallback.accept(dockerCube);
            }
            cubeController.create(containerName);
            cubeController.start(containerName);
        }
    }

    private boolean isNotInitialized() {
        return cubeRegistry.getCube(containerName) == null;
    }

    private void enrichContainerObjectWithCube() throws IllegalAccessException {
        enrichAnnotatedPortBuildingFields(CubeIp.class, DockerContainerObjectBuilder::findEnrichedValueForFieldWithCubeIpAnnotation);
        enrichAnnotatedPortBuildingFields(HostPort.class, DockerContainerObjectBuilder::findEnrichedValueForFieldWithHostPortAnnotation);
    }

    private <T extends Annotation> void enrichAnnotatedPortBuildingFields(Class<T> annotationType, BiFunction<T, HasPortBindings, ?> fieldEnricher) throws IllegalAccessException {
        final List<Field> annotatedFields = ReflectionUtil.getFieldsWithAnnotation(containerObjectClass, annotationType);
        if (annotatedFields.isEmpty()) return;

        final HasPortBindings portBindings = dockerCube.getMetadata(HasPortBindings.class);
        if (portBindings == null) {
            throw new IllegalArgumentException(String.format("Container Object %s contains fields annotated with %s but no ports are exposed by the container", containerObjectClass.getSimpleName(), annotationType.getSimpleName()));
        }

        for (Field annotatedField: annotatedFields) {
            final T annotation = annotatedField.getAnnotation(annotationType);
            try {
                annotatedField.set(containerObjectInstance, fieldEnricher.apply(annotation, portBindings));
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException(
                        String.format("Container Object %s contains field %s annotated with %s, with error: %s", containerObjectClass.getSimpleName(), annotatedField.getName(), annotationType.getSimpleName(), ex.getLocalizedMessage()),
                        ex);
            }
        }
    }

    private static String findEnrichedValueForFieldWithCubeIpAnnotation(CubeIp cubeIp, HasPortBindings portBindings) {
        final boolean cubeIpInternal = cubeIp.internal();
        final String cubeIpValue = (cubeIpInternal) ? portBindings.getInternalIP() : portBindings.getContainerIP();
        return cubeIpValue;
    }

    private static int findEnrichedValueForFieldWithHostPortAnnotation(HostPort hostPort, HasPortBindings portBindings) {
        int hostPortValue = hostPort.value();
        if (hostPortValue == 0) {
            throw new IllegalArgumentException(String.format("%s annotation does not specify any exposed port", HostPort.class.getSimpleName()));
        }
        final HasPortBindings.PortAddress bindingForExposedPort = portBindings.getMappedAddress(hostPortValue);
        if (bindingForExposedPort == null) {
            throw new IllegalArgumentException(String.format("exposed port %s is not exposed on container object.", hostPortValue));
        }
        return bindingForExposedPort.getPort();
    }

    private static File createTemporalDirectoryForCopyingDockerfile(String containerName) throws IOException {
        File dir = File.createTempFile(TEMPORARY_FOLDER_PREFIX+containerName, TEMPORARY_FOLDER_SUFFIX);
        dir.delete();
        if (!dir.mkdirs()) {
            throw new IllegalArgumentException("Temp Dir for storing Dockerfile contents could not be created.");
        }
        dir.deleteOnExit();
        return dir;
    }

    private static Link linkFromCubeAnnotatedField(Field cubeField) {
        final String linkName = linkNameFromCubeAnnotatedField(cubeField);
        return Link.valueOf(linkName);
    }

    private static String linkNameFromCubeAnnotatedField(Field cubeField) {
        if (cubeField.isAnnotationPresent(org.arquillian.cube.containerobject.Link.class)) {
            return cubeField.getAnnotation(org.arquillian.cube.containerobject.Link.class).value();
        }

        final String cubeName = cubeNameFromCubeAnnotatedField(cubeField);
        return cubeName + ":" + cubeName;
    }

    private static String cubeNameFromCubeAnnotatedField(Field cubeField) {
        final org.arquillian.cube.containerobject.Cube cubeAnnotationOnField = cubeField.getAnnotation(org.arquillian.cube.containerobject.Cube.class);
        final Class<?> containerObjectClassFromField = cubeField.getType();
        String cubeName = null;
        final String cubeAnnotationOnFieldValue = cubeAnnotationOnField.value();
        if (cubeAnnotationOnFieldValue != null && !Cube.DEFAULT_VALUE.equals(cubeAnnotationOnFieldValue)) {
            // We have found a valid cube name
            return cubeAnnotationOnFieldValue;
        }
        // Needs to check if container object or one of his parents contains a Cube
        final String cubeAnnotationOnClassValue = ContainerObjectUtil.getTopCubeAttribute(containerObjectClassFromField, "value", Cube.class, Cube.DEFAULT_VALUE);
        if (cubeAnnotationOnClassValue != null && !Cube.DEFAULT_VALUE.equals(cubeAnnotationOnClassValue)) {
            //We got the cubeName in containerobject
            return cubeAnnotationOnClassValue;
        }

        //No override so we need to use the default logic
        return containerObjectClassFromField.getSimpleName();
    }
}
