package org.arquillian.cube.docker.junit5;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.arquillian.cube.docker.impl.client.SystemPropertiesCubeSetter;
import org.arquillian.cube.docker.impl.client.containerobject.dsl.Container;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.docker.impl.model.DockerCube;
import org.arquillian.cube.docker.junit.DockerClientInitializer;
import org.arquillian.cube.docker.junit.Injector;
import org.arquillian.cube.docker.junit.Reflections;
import org.arquillian.cube.impl.model.LocalCubeRegistry;
import org.arquillian.cube.spi.event.lifecycle.AfterDestroy;
import org.arquillian.cube.spi.event.lifecycle.AfterStart;
import org.jboss.arquillian.test.spi.event.suite.AfterSuite;
import org.jboss.arquillian.test.spi.event.suite.BeforeSuite;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class ContainerDslResolver implements BeforeAllCallback, BeforeEachCallback, AfterAllCallback,
    AfterEachCallback {

    private final DockerClientExecutor dockerClientExecutor;
    private final SystemPropertiesCubeSetter systemPropertiesCubeSetter = new SystemPropertiesCubeSetter();

    private final List<DockerCube> cubesPerMethod = new ArrayList<>();
    private final List<DockerCube> cubesPerClass = new ArrayList<>();

    public ContainerDslResolver() {
        this.dockerClientExecutor = DockerClientInitializer.initialize();
        // Since we are out of arquillian runner we need to call events by ourselves.
        this.systemPropertiesCubeSetter.createDockerHostProperty(new BeforeSuite(), this.dockerClientExecutor);
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) {
        after(this.cubesPerClass);
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) {
        after(this.cubesPerMethod);
    }

    private void after(List<DockerCube> dockerCubes) {

        for (DockerCube dockerCube : dockerCubes) {
            // stop container
            dockerCube.stop();
            dockerCube.destroy();

            systemPropertiesCubeSetter.removeCubeSystemProperties(new AfterDestroy(dockerCube.getId()));
            systemPropertiesCubeSetter.removeDockerHostProperty(new AfterSuite());
        }

        dockerCubes.clear();
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {

        final Class<?> testClass = extensionContext.getRequiredTestClass();
        final Object testInstance = extensionContext.getTestInstance().orElse(null);
        final List<Field> allStaticContainerDslFields =
            Reflections.findAllFieldsOfType(testClass, ContainerDsl.class, f -> Modifier
                .isStatic(f.getModifiers()));

        cubesPerClass.addAll(before(testInstance, allStaticContainerDslFields));
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {

        final Class<?> testClass = extensionContext.getRequiredTestClass();
        final Object testInstance = extensionContext.getTestInstance().orElse(null);
        final List<Field> allContainerDslFields =
            Reflections.findAllFieldsOfType(testClass, ContainerDsl.class, f -> !Modifier
                .isStatic(f.getModifiers()));

        cubesPerMethod.addAll(before(testInstance, allContainerDslFields));
    }

    private List<DockerCube> before(Object testInstance, List<Field> allStaticContainerDslFields)
        throws Exception {
        final List<DockerCube> createdCubes = new ArrayList<>();

        final LocalCubeRegistry localCubeRegistry = new LocalCubeRegistry();
        for (Field containerDslField : allStaticContainerDslFields) {

            final ContainerDsl containerDsl = (ContainerDsl) containerDslField.get(testInstance);
            final Container container = containerDsl.buildContainer();
            final DockerCube dockerCube = Injector.prepareContainer(container, dockerClientExecutor, localCubeRegistry);
            createdCubes.add(dockerCube);

            dockerCube.create();
            dockerCube.start();

            systemPropertiesCubeSetter.createCubeSystemProperties(new AfterStart(dockerCube.getId()), localCubeRegistry);
        }

        return createdCubes;
    }
}
