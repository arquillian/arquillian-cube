package org.arquillian.cube.docker.impl.client.containerobject;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import com.google.common.base.StandardSystemProperty;

import org.apache.commons.io.FileUtils;
import org.arquillian.cube.CubeController;
import org.arquillian.cube.containerobject.Cube;
import org.arquillian.cube.containerobject.CubeDockerFile;
import org.arquillian.cube.containerobject.Environment;
import org.arquillian.cube.containerobject.Image;
import org.arquillian.cube.containerobject.Link;
import org.arquillian.cube.containerobject.Volume;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.docker.impl.model.DockerCube;
import org.arquillian.cube.impl.model.LocalCubeRegistry;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.metadata.IsContainerObject;
import org.jboss.arquillian.core.api.Injector;
import org.jboss.arquillian.core.spi.ServiceLoader;
import org.jboss.arquillian.test.spi.TestEnricher;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.GenericArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.docker.DockerDescriptor;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

public class CubeContainerObjectTestEnricherTest {

    private CubeRegistry cubeRegistry;
    private CubeController cubeController;
    private DockerClientExecutor dockerClientExecutor;
    private Injector injector;
    private ServiceLoader serviceLoader;
    private DockerContainerObjectFactory dockerContainerObjectFactory;

    @AfterClass
    public static void cleanEnvironment() {
        deleteTestDirectory();
    }

    @Before
    public void init() {
        cubeRegistry = new LocalCubeRegistry();
        dockerContainerObjectFactory = new DockerContainerObjectFactory();
        cubeController = mock(CubeController.class);
        dockerClientExecutor = mock(DockerClientExecutor.class);
        injector = mock(Injector.class);
        serviceLoader = mock(ServiceLoader.class);
        when(serviceLoader.all(any(Class.class))).thenReturn(Collections.emptyList());

        dockerContainerObjectFactory.serviceLoaderInstance = () -> serviceLoader;
        dockerContainerObjectFactory.dockerClientExecutorInstance = () -> dockerClientExecutor;
        dockerContainerObjectFactory.cubeRegistryInstance = () -> cubeRegistry;
        dockerContainerObjectFactory.cubeControllerInstance = () -> cubeController;
        dockerContainerObjectFactory.injectorInstance = () -> injector;

        //We asure that there is no previous executions there.
        deleteTestDirectory();
    }

    @Test
    public void shouldStartAContainerObject() {
        CubeContainerObjectTestEnricher cubeContainerObjectTestEnricher = new CubeContainerObjectTestEnricher();
        cubeContainerObjectTestEnricher.containerObjectFactoryInstance = () -> dockerContainerObjectFactory;
        when(serviceLoader.all(TestEnricher.class)).thenReturn(Arrays.asList(cubeContainerObjectTestEnricher));

        InjectableTest injectableTest = new InjectableTest();
        cubeContainerObjectTestEnricher.enrich(injectableTest);

        final org.arquillian.cube.spi.Cube<?> mycontainer = cubeRegistry.getCube("mycontainer");
        assertThat(mycontainer, is(notNullValue()));
        assertThat(mycontainer.hasMetadata(IsContainerObject.class), is(true));
        assertThat(mycontainer.getMetadata(IsContainerObject.class).getTestClass().getName(), is(InjectableTest.class.getName()));

        verify(cubeController, times(1)).start("mycontainer");
        verify(cubeController, times(1)).create("mycontainer");

    }

    @Test
    public void shouldShouldStartAContainerObjectDefinedUsingDescriptor() {
        CubeContainerObjectTestEnricher cubeContainerObjectTestEnricher = new CubeContainerObjectTestEnricher();
        cubeContainerObjectTestEnricher.containerObjectFactoryInstance = () -> dockerContainerObjectFactory;
        when(serviceLoader.all(TestEnricher.class)).thenReturn(Arrays.asList(cubeContainerObjectTestEnricher));

        SecondInjectableTest secondInjectableTest = new SecondInjectableTest();
        cubeContainerObjectTestEnricher.enrich(secondInjectableTest);

        final org.arquillian.cube.spi.Cube<?> mycontainer = cubeRegistry.getCube("mycontainer2");
        assertThat(mycontainer, is(notNullValue()));

        verify(cubeController, times(1)).start("mycontainer2");
        verify(cubeController, times(1)).create("mycontainer2");

        final File generatedDirectory = findGeneratedDirectory();
        assertThat(generatedDirectory, is(notNullValue()));
        assertThat(new File(generatedDirectory, "Dockerfile").exists(), is(true));
    }

    @Test
    public void shouldLinkInnerContainers() {
        CubeContainerObjectTestEnricher cubeContainerObjectTestEnricher = new CubeContainerObjectTestEnricher();
        cubeContainerObjectTestEnricher.containerObjectFactoryInstance = () -> dockerContainerObjectFactory;
        when(serviceLoader.all(TestEnricher.class)).thenReturn(Arrays.asList(cubeContainerObjectTestEnricher));

        ThirdInjetableTest thirdInjetableTest = new ThirdInjetableTest();
        cubeContainerObjectTestEnricher.enrich(thirdInjetableTest);

        final org.arquillian.cube.spi.Cube<?> outterContainer = cubeRegistry.getCube("outter");
        assertThat(outterContainer, is(notNullValue()));

        DockerCube dockerCube = (DockerCube) outterContainer;
        final Collection<org.arquillian.cube.docker.impl.client.config.Link> links = dockerCube.configuration().getLinks();
        assertThat(links.size(), is(1));
        assertThat(links, hasItem(org.arquillian.cube.docker.impl.client.config.Link.valueOf("db:db")));

        final org.arquillian.cube.spi.Cube<?> innerContainer = cubeRegistry.getCube("inner");
        assertThat(innerContainer, is(notNullValue()));

        verify(cubeController, times(1)).start("outter");
        verify(cubeController, times(1)).create("outter");

        verify(cubeController, times(1)).start("inner");
        verify(cubeController, times(1)).create("inner");
    }

    @Test
    public void shouldLinkInnerContainersWithoutLink() {
        CubeContainerObjectTestEnricher cubeContainerObjectTestEnricher = new CubeContainerObjectTestEnricher();
        cubeContainerObjectTestEnricher.containerObjectFactoryInstance = () -> dockerContainerObjectFactory;
        when(serviceLoader.all(TestEnricher.class)).thenReturn(Arrays.asList(cubeContainerObjectTestEnricher));

        FifthInjetableTest fifthInjetableTest = new FifthInjetableTest();
        cubeContainerObjectTestEnricher.enrich(fifthInjetableTest);

        final org.arquillian.cube.spi.Cube<?> outterContainer = cubeRegistry.getCube("outter");
        assertThat(outterContainer, is(notNullValue()));

        DockerCube dockerCube = (DockerCube) outterContainer;
        final Collection<org.arquillian.cube.docker.impl.client.config.Link> links = dockerCube.configuration().getLinks();
        assertThat(links.size(), is(1));
        assertThat(links, hasItem(org.arquillian.cube.docker.impl.client.config.Link.valueOf("inner:inner")));

        final org.arquillian.cube.spi.Cube<?> innerContainer = cubeRegistry.getCube("inner");
        assertThat(innerContainer, is(notNullValue()));

        verify(cubeController, times(1)).start("outter");
        verify(cubeController, times(1)).create("outter");

        verify(cubeController, times(1)).start("inner");
        verify(cubeController, times(1)).create("inner");
    }

    @Test
    public void shouldStartAContainerObjectDefinedAsImage() {
        CubeContainerObjectTestEnricher cubeContainerObjectTestEnricher = new CubeContainerObjectTestEnricher();
        cubeContainerObjectTestEnricher.containerObjectFactoryInstance = () -> dockerContainerObjectFactory;
        when(serviceLoader.all(TestEnricher.class)).thenReturn(Arrays.asList(cubeContainerObjectTestEnricher));

        FourthInjectableTest injectableTest = new FourthInjectableTest();
        cubeContainerObjectTestEnricher.enrich(injectableTest);

        final org.arquillian.cube.spi.Cube<?> image = cubeRegistry.getCube("image");
        assertThat(image, is(notNullValue()));
        assertThat(image.hasMetadata(IsContainerObject.class), is(true));
        assertThat(image.getMetadata(IsContainerObject.class).getTestClass().getName(), is(FourthInjectableTest.class.getName()));

        verify(cubeController, times(1)).start("image");
        verify(cubeController, times(1)).create("image");

        DockerCube dockerCube = (DockerCube) image;
        assertThat(dockerCube.configuration().getImage().toImageRef(), is("tomee:8-jre-1.7.2-webprofile"));

    }

    @Test
    public void shouldStartAContainerObjectDefinedAsImageAndEnvironmentVariables() {
        CubeContainerObjectTestEnricher cubeContainerObjectTestEnricher = new CubeContainerObjectTestEnricher();
        cubeContainerObjectTestEnricher.containerObjectFactoryInstance = () -> dockerContainerObjectFactory;
        when(serviceLoader.all(TestEnricher.class)).thenReturn(Arrays.asList(cubeContainerObjectTestEnricher));

        FifthInjectableTest injectableTest = new FifthInjectableTest();
        cubeContainerObjectTestEnricher.enrich(injectableTest);

        final org.arquillian.cube.spi.Cube<?> image = cubeRegistry.getCube("image");
        assertThat(image, is(notNullValue()));
        assertThat(image.hasMetadata(IsContainerObject.class), is(true));
        assertThat(image.getMetadata(IsContainerObject.class).getTestClass().getName(), is(FifthInjectableTest.class.getName()));

        verify(cubeController, times(1)).start("image");
        verify(cubeController, times(1)).create("image");

        DockerCube dockerCube = (DockerCube) image;
        assertThat(dockerCube.configuration().getImage().toImageRef(), is("tomee:8-jre-1.7.2-webprofile"));
        assertThat(dockerCube.configuration().getEnv(), hasItems("a=b", "c=d"));

    }

    @Test
    public void shouldStartAContainerObjectDefinedAsImageAndVolumesVariables() {
        CubeContainerObjectTestEnricher cubeContainerObjectTestEnricher = new CubeContainerObjectTestEnricher();
        cubeContainerObjectTestEnricher.containerObjectFactoryInstance = () -> dockerContainerObjectFactory;
        when(serviceLoader.all(TestEnricher.class)).thenReturn(Arrays.asList(cubeContainerObjectTestEnricher));

        SixthInjectableTest injectableTest = new SixthInjectableTest();
        cubeContainerObjectTestEnricher.enrich(injectableTest);

        final org.arquillian.cube.spi.Cube<?> image = cubeRegistry.getCube("image");
        assertThat(image, is(notNullValue()));
        assertThat(image.hasMetadata(IsContainerObject.class), is(true));
        assertThat(image.getMetadata(IsContainerObject.class).getTestClass().getName(), is(SixthInjectableTest.class.getName()));

        verify(cubeController, times(1)).start("image");
        verify(cubeController, times(1)).create("image");

        DockerCube dockerCube = (DockerCube) image;
        assertThat(dockerCube.configuration().getImage().toImageRef(), is("tomee:8-jre-1.7.2-webprofile"));
        assertThat(dockerCube.configuration().getBinds(), hasItems("/mypath:/containerPath:rw", "/mypath2:/containerPath2:rw"));

    }

    private static class InjectableTest {
        @Cube(value = "mycontainer")
        TestContainerObject testContainerObject;
    }

    private static class SecondInjectableTest {
        @Cube("mycontainer2")
        TestInnerDockerfileContainerObject testInnerDockerfileContainerObject;
    }

    private static class ThirdInjetableTest {
        @Cube("outter")
        TestLinkedContainerObject testLinkedContainerObject;
    }

    private static class FifthInjetableTest {
        @Cube("outter")
        TestLinkedContainerObjectNoLink testLinkedContainerObjectNoLink;
    }

    private static class FourthInjectableTest {
        @Cube("image")
        ImageContainerObject imageContainerObject;
    }
    private static class FifthInjectableTest {
        @Cube("image")
        ImageWithEnvContainerObject imageContainerObject;
    }
    private static class SixthInjectableTest {
        @Cube("image")
        @Volume(hostPath = "/mypath2", containerPath = "/containerPath2")
        VolumesContainerObject volumesContainerObject;
    }

    @CubeDockerFile
    public static class TestContainerObject {
        public TestContainerObject() {
        }
    }

    public static class TestInnerDockerfileContainerObject {
        @CubeDockerFile
        public static Archive<?> createDockerfile() {
            String dockerDescriptor = Descriptors.create(DockerDescriptor.class).from("tomee").exportAsString();
            return ShrinkWrap.create(GenericArchive.class)
                    .add(new StringAsset(dockerDescriptor), "Dockerfile");
        }
    }

    public static class TestLinkedContainerObject {

        @CubeDockerFile
        public static Archive<?> createDockerfile() {
            String dockerDescriptor = Descriptors.create(DockerDescriptor.class).from("tomee").exportAsString();
            return ShrinkWrap.create(GenericArchive.class)
                    .add(new StringAsset(dockerDescriptor), "Dockerfile");
        }

        @Cube("inner")
        @Link("db:db")
        TestLinkContainerObject linkContainerObject;

    }

    public static class TestLinkedContainerObjectNoLink {

        @CubeDockerFile
        public static Archive<?> createDockerfile() {
            String dockerDescriptor = Descriptors.create(DockerDescriptor.class).from("tomee").exportAsString();
            return ShrinkWrap.create(GenericArchive.class)
                    .add(new StringAsset(dockerDescriptor), "Dockerfile");
        }

        @Cube("inner")
        TestLinkContainerObject linkContainerObject;

    }

    @Image("tomee:8-jre-1.7.2-webprofile")
    public static class ImageContainerObject {
    }

    @Image("tomee:8-jre-1.7.2-webprofile")
    @Environment(key = "a", value = "b")
    @Environment(key = "c",  value = "d")
    public static class ImageWithEnvContainerObject {
    }

    @Image("tomee:8-jre-1.7.2-webprofile")
    @Volume(hostPath = "/mypath", containerPath = "/containerPath")
    public static class VolumesContainerObject{
    }

    public static class TestLinkContainerObject {
        @CubeDockerFile
        public static Archive<?> createDockerfile() {
            String dockerDescriptor = Descriptors.create(DockerDescriptor.class).from("mysql").exportAsString();
            return ShrinkWrap.create(GenericArchive.class)
                    .add(new StringAsset(dockerDescriptor), "Dockerfile");
        }
    }

    private static void deleteTestDirectory() {
        File tempDirectory = new File(StandardSystemProperty.JAVA_IO_TMPDIR.value());
        final File[] testsDirectories = tempDirectory.listFiles(CubeContainerObjectTestEnricherTest::testDirectoryFilter);
        for (File testDirectory: testsDirectories) {
            try {
                FileUtils.deleteDirectory(testDirectory);
            } catch (IOException e) {
                // ignore
            }
        }
    }

    private static File findGeneratedDirectory() {
        File tempDirectory = new File(StandardSystemProperty.JAVA_IO_TMPDIR.value());
        final File[] testsDirectories = tempDirectory.listFiles(CubeContainerObjectTestEnricherTest::testDirectoryFilter);
        if (testsDirectories.length > 0) {
            return testsDirectories[0];
        } else {
            return null;
        }
    }

    private static boolean testDirectoryFilter(File dir, String name) {
        return dir.isDirectory()
                && name.startsWith(DockerContainerObjectBuilder.TEMPORARY_FOLDER_PREFIX)
                && name.endsWith(DockerContainerObjectBuilder.TEMPORARY_FOLDER_SUFFIX);
    }
}
