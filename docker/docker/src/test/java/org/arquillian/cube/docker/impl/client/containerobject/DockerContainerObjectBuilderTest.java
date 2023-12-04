package org.arquillian.cube.docker.impl.client.containerobject;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import com.github.dockerjava.api.DockerClient;
import org.apache.commons.io.FileUtils;
import org.arquillian.cube.CubeController;
import org.arquillian.cube.CubeIp;
import org.arquillian.cube.HostPort;
import org.arquillian.cube.containerobject.Cube;
import org.arquillian.cube.containerobject.CubeDockerFile;
import org.arquillian.cube.containerobject.Environment;
import org.arquillian.cube.containerobject.Image;
import org.arquillian.cube.containerobject.Link;
import org.arquillian.cube.containerobject.Volume;
import org.arquillian.cube.docker.impl.client.config.CubeContainer;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.docker.impl.model.DockerCube;
import org.arquillian.cube.impl.util.ReflectionUtil;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.metadata.HasPortBindings;
import org.arquillian.cube.spi.metadata.IsContainerObject;
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
import org.mockito.invocation.InvocationOnMock;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DockerContainerObjectBuilderTest {

    public static final String BASE_IMAGE = "tomee:8-jre-1.7.2-webprofile";

    private CubeController cubeController;
    private DockerClientExecutor dockerClientExecutor;
    private CubeContainerObjectTestEnricher cubeContainerObjectTestEnricher;
    private Collection<TestEnricher> enrichers;
    private CubeRegistry cubeRegistry;

    @Before
    public void initMocks() {
        cubeController = mock(CubeController.class);
        dockerClientExecutor = mock(DockerClientExecutor.class);
        when(dockerClientExecutor.isDockerInsideDockerResolution()).thenReturn(true);
        when(dockerClientExecutor.getDockerClient()).thenReturn(mock(DockerClient.class, RETURNS_DEEP_STUBS));
        cubeRegistry = mock(CubeRegistry.class);
        cubeContainerObjectTestEnricher = mock(CubeContainerObjectTestEnricher.class);
        doAnswer(DockerContainerObjectBuilderTest::objectContainerEnricherMockEnrich)
                .when(cubeContainerObjectTestEnricher).enrich(any());
        enrichers = Collections.singleton(cubeContainerObjectTestEnricher);
    }

    @Before
    public void cleanupTestDirsBeforeEachTest() {
        deleteTestDirectory();
    }

    @AfterClass
    public static void cleanupTestDirsWhenDone() {
        deleteTestDirectory();
    }

    @Test
    public void shouldStartAContainerObjectDefinedUsingDockerfile() {
        final AtomicReference<DockerCube> cubeRef = new AtomicReference<>();
        try {
            TestContainerObjectDefinedUsingDockerfile containerObject = new DockerContainerObjectBuilder<TestContainerObjectDefinedUsingDockerfile>(dockerClientExecutor, cubeController, cubeRegistry)
                    .withContainerObjectClass(TestContainerObjectDefinedUsingDockerfile.class)
                    .onCubeCreated(cubeRef::set)
                    .build();
            assertThat(containerObject, is(notNullValue()));
        } catch (IllegalAccessException|InvocationTargetException|IOException e) {
            fail();
        }

        DockerCube cube = cubeRef.get();
        assertThat(cube, is(notNullValue()));
        assertThat(cube.hasMetadata(IsContainerObject.class), is(true));
        assertThat(cube.getMetadata(IsContainerObject.class).getTestClass(), is(nullValue()));

        verify(cubeController, times(1)).create("containerDefinedUsingDockerfile");
        verify(cubeController, times(1)).start("containerDefinedUsingDockerfile");
    }

    @Test
    public void shouldStartAContainerObjectDefinedUsingDescriptor() {
        final AtomicReference<DockerCube> cubeRef = new AtomicReference<>();
        try {
            TestContainerObjectDefinedUsingDescriptor containerObject = new DockerContainerObjectBuilder<TestContainerObjectDefinedUsingDescriptor>(
                        dockerClientExecutor, cubeController, cubeRegistry)
                    .withContainerObjectClass(TestContainerObjectDefinedUsingDescriptor.class)
                    .onCubeCreated(cubeRef::set)
                    .build();
            assertThat(containerObject, is(notNullValue()));
        } catch (IllegalAccessException|InvocationTargetException|IOException e) {
            fail();
        }

        DockerCube cube = cubeRef.get();
        assertThat(cube, is(notNullValue()));
        assertThat(cube.hasMetadata(IsContainerObject.class), is(true));
        assertThat(cube.getMetadata(IsContainerObject.class).getTestClass(), is(nullValue()));

        verify(cubeController, times(1)).create("containerDefinedUsingDescriptor");
        verify(cubeController, times(1)).start("containerDefinedUsingDescriptor");

        final File generatedDirectory = findGeneratedDirectory();
        assertThat(generatedDirectory, is(notNullValue()));
        assertThat(new File(generatedDirectory, "Dockerfile").exists(), is(true));
    }

    @Test
    public void shouldLinkInnerContainersWithLink() {
        final AtomicReference<DockerCube> cubeRef = new AtomicReference<>();
        try {
            TestContainerObjectWithAnnotatedLink containerObject = new DockerContainerObjectBuilder<TestContainerObjectWithAnnotatedLink>(
                        dockerClientExecutor, cubeController, cubeRegistry)
                    .withContainerObjectClass(TestContainerObjectWithAnnotatedLink.class)
                    .withEnrichers(enrichers)
                    .onCubeCreated(cubeRef::set)
                    .build();
            assertThat(containerObject, is(notNullValue()));
            assertThat(containerObject.linkedContainerObject, is(notNullValue()));
        } catch (IllegalAccessException|InvocationTargetException|IOException e) {
            fail();
        }

        DockerCube cube = cubeRef.get();
        assertThat(cube, is(notNullValue()));
        assertThat(cube.hasMetadata(IsContainerObject.class), is(true));
        assertThat(cube.getMetadata(IsContainerObject.class).getTestClass(), is(nullValue()));
        Collection<org.arquillian.cube.docker.impl.client.config.Link> links = cube.configuration().getLinks();
        assertThat(links, is(notNullValue()));
        assertThat(links.size(), is(1));
        assertThat(links, hasItem(org.arquillian.cube.docker.impl.client.config.Link.valueOf("db:db")));

        verify(cubeController, times(1)).create("containerWithAnnotatedLink");
        verify(cubeController, times(1)).start("containerWithAnnotatedLink");

        verify(cubeContainerObjectTestEnricher, times(1)).enrich(any(TestContainerObjectWithAnnotatedLink.class));
    }

    @Test
    public void shouldLinkInnerContainersWithoutLink() {
        final AtomicReference<DockerCube> cubeRef = new AtomicReference<>();
        try {
            TestContainerObjectWithNonAnnotatedLink containerObject = new DockerContainerObjectBuilder<TestContainerObjectWithNonAnnotatedLink>(dockerClientExecutor, cubeController, cubeRegistry)
                    .withContainerObjectClass(TestContainerObjectWithNonAnnotatedLink.class)
                    .withEnrichers(enrichers)
                    .onCubeCreated(cubeRef::set)
                    .build();
            assertThat(containerObject, is(notNullValue()));
            assertThat(containerObject.linkedContainerObject, is(notNullValue()));
        } catch (IllegalAccessException|InvocationTargetException|IOException e) {
            fail();
        }

        DockerCube cube = cubeRef.get();
        assertThat(cube, is(notNullValue()));
        assertThat(cube.hasMetadata(IsContainerObject.class), is(true));
        assertThat(cube.getMetadata(IsContainerObject.class).getTestClass(), is(nullValue()));
        Collection<org.arquillian.cube.docker.impl.client.config.Link> links = cube.configuration().getLinks();
        assertThat(links, is(notNullValue()));
        assertThat(links.size(), is(1));
        assertThat(links, hasItem(org.arquillian.cube.docker.impl.client.config.Link.valueOf("inner:inner")));

        verify(cubeController, times(1)).create("containerWithNonAnnotatedLink");
        verify(cubeController, times(1)).start("containerWithNonAnnotatedLink");

        verify(cubeContainerObjectTestEnricher, times(1)).enrich(any(TestContainerObjectWithAnnotatedLink.class));
    }

    @Test
    public void shouldStartAContainerObjectDefinedUsingImage() {
        final AtomicReference<DockerCube> cubeRef = new AtomicReference<>();
        try {
            TestContainerObjectDefinedUsingImage containerObject = new DockerContainerObjectBuilder<TestContainerObjectDefinedUsingImage>(dockerClientExecutor, cubeController, cubeRegistry)
                    .withContainerObjectClass(TestContainerObjectDefinedUsingImage.class)
                    .onCubeCreated(cubeRef::set)
                    .build();
            assertThat(containerObject, is(notNullValue()));
        } catch (IllegalAccessException|InvocationTargetException|IOException e) {
            fail();
        }

        DockerCube cube = cubeRef.get();
        assertThat(cube, is(notNullValue()));
        assertThat(cube.hasMetadata(IsContainerObject.class), is(true));
        assertThat(cube.getMetadata(IsContainerObject.class).getTestClass(), is(nullValue()));
        assertThat(cube.configuration().getImage().toImageRef(), is(BASE_IMAGE));

        verify(cubeController, times(1)).create("containerDefinedUsingImage");
        verify(cubeController, times(1)).start("containerDefinedUsingImage");
    }

    @Test
    public void shouldStartAContainerObjectDefinedUsingImageAndEnvironmentVariables() {
        final AtomicReference<DockerCube> cubeRef = new AtomicReference<>();
        try {
            CubeContainer ccconfig = new CubeContainer();
            ccconfig.setEnv(Collections.singleton("e=f"));
            CubeContainerObjectConfiguration ccoconfig = new CubeContainerObjectConfiguration(ccconfig);
            TestContainerObjectDefinedUsingImageAndEnvironmentVariables containerObject = new DockerContainerObjectBuilder<TestContainerObjectDefinedUsingImageAndEnvironmentVariables>(dockerClientExecutor, cubeController, cubeRegistry)
                    .withContainerObjectClass(TestContainerObjectDefinedUsingImageAndEnvironmentVariables.class)
                    .withContainerObjectConfiguration(ccoconfig)
                    .onCubeCreated(cubeRef::set)
                    .build();
            assertThat(containerObject, is(notNullValue()));
        } catch (IllegalAccessException|InvocationTargetException|IOException e) {
            fail();
        }

        DockerCube cube = cubeRef.get();
        assertThat(cube, is(notNullValue()));
        assertThat(cube.hasMetadata(IsContainerObject.class), is(true));
        assertThat(cube.getMetadata(IsContainerObject.class).getTestClass(), is(nullValue()));
        assertThat(cube.configuration().getImage().toImageRef(), is(BASE_IMAGE));
        assertThat(cube.configuration().getEnv(), hasItems("a=b", "c=d", "e=f"));

        verify(cubeController, times(1)).create("containerWithEnvironmentVariables");
        verify(cubeController, times(1)).start("containerWithEnvironmentVariables");
    }

    @Test
    public void shouldStartAContainerObjectDefinedUsingImageAndVolumes() {
        final AtomicReference<DockerCube> cubeRef = new AtomicReference<>();
        try {
            CubeContainer ccconfig = new CubeContainer();
            ccconfig.setBinds(Collections.singleton("/mypath3:/containerPath3:Z"));
            CubeContainerObjectConfiguration ccoconfig = new CubeContainerObjectConfiguration(ccconfig);
            TestContainerObjectDefinedUsingImageAndVolumes containerObject = new DockerContainerObjectBuilder<TestContainerObjectDefinedUsingImageAndVolumes>(dockerClientExecutor, cubeController, cubeRegistry)
                    .withContainerObjectClass(TestContainerObjectDefinedUsingImageAndVolumes.class)
                    .withContainerObjectConfiguration(ccoconfig)
                    .onCubeCreated(cubeRef::set)
                    .build();
            assertThat(containerObject, is(notNullValue()));
        } catch (IllegalAccessException|InvocationTargetException|IOException e) {
            fail();
        }

        DockerCube cube = cubeRef.get();
        assertThat(cube, is(notNullValue()));
        assertThat(cube.hasMetadata(IsContainerObject.class), is(true));
        assertThat(cube.getMetadata(IsContainerObject.class).getTestClass(), is(nullValue()));
        assertThat(cube.configuration().getImage().toImageRef(), is(BASE_IMAGE));
        assertThat(cube.configuration().getBinds(), hasItems("/mypath:/containerPath:Z", "/mypath2:/containerPath2:Z", "/mypath3:/containerPath3:Z"));

        verify(cubeController, times(1)).create("containerWithVolumes");
        verify(cubeController, times(1)).start("containerWithVolumes");
    }

    @Test
    public void shouldEnrichAContainerWithCubeIp() {
        final AtomicReference<DockerCube> cubeRef = new AtomicReference<>();
        doAnswer(invocation -> {
            // when started, initialize cube port bindings
            initDockerCubeInternalIP(cubeRef.get(), "172.17.0.2");
            return null;
        }).when(cubeController).start("containerWithCubeIp");
        try {
            TestContainerObjectWithCubeIp containerObject = new DockerContainerObjectBuilder<TestContainerObjectWithCubeIp>(
                        dockerClientExecutor, cubeController, cubeRegistry)
                    .withContainerObjectClass(TestContainerObjectWithCubeIp.class)
                    .onCubeCreated(cubeRef::set)
                    .build();
            assertThat(containerObject, is(notNullValue()));
            assertThat(containerObject.cubeIp, is("172.17.0.2"));
        } catch (IllegalAccessException|InvocationTargetException|IOException e) {
            fail();
        }

        DockerCube cube = cubeRef.get();
        assertThat(cube, is(notNullValue()));
        assertThat(cube.hasMetadata(IsContainerObject.class), is(true));
        assertThat(cube.getMetadata(IsContainerObject.class).getTestClass(), is(nullValue()));
        assertThat(cube.hasMetadata(HasPortBindings.class), is(true));

        verify(cubeController, times(1)).create("containerWithCubeIp");
        verify(cubeController, times(1)).start("containerWithCubeIp");
    }

    @Test
    public void shouldEnrichAContainerWithHostPort() {
        final AtomicReference<DockerCube> cubeRef = new AtomicReference<>();
        doAnswer(invocation -> {
            // when started, initialize cube port bindings
            initDockerCubeMappedPort(cubeRef.get(), "127.0.0.1", 8080, 8080);
            return null;
        }).when(cubeController).start("containerWithHostPort");
        try {
            TestContainerObjectWithHostPort containerObject = new DockerContainerObjectBuilder<TestContainerObjectWithHostPort>(
                    dockerClientExecutor, cubeController, cubeRegistry)
                    .withContainerObjectClass(TestContainerObjectWithHostPort.class)
                    .onCubeCreated(cubeRef::set)
                    .build();
            assertThat(containerObject, is(notNullValue()));
            assertThat(containerObject.port, is(8080));
        } catch (IllegalAccessException|InvocationTargetException|IOException e) {
            fail();
        }

        DockerCube cube = cubeRef.get();
        assertThat(cube, is(notNullValue()));
        assertThat(cube.hasMetadata(IsContainerObject.class), is(true));
        assertThat(cube.getMetadata(IsContainerObject.class).getTestClass(), is(nullValue()));
        assertThat(cube.hasMetadata(HasPortBindings.class), is(true));

        verify(cubeController, times(1)).create("containerWithHostPort");
        verify(cubeController, times(1)).start("containerWithHostPort");
    }

    //<editor-fold desc="container object classes used by test methods">

    @Cube("containerDefinedUsingDockerfile")
    @CubeDockerFile
    public static class TestContainerObjectDefinedUsingDockerfile {
    }

    @Cube("containerDefinedUsingDescriptor")
    public static class TestContainerObjectDefinedUsingDescriptor {

        @CubeDockerFile
        public static Archive<?> createDockerfile() {
            String dockerDescriptor = Descriptors.create(DockerDescriptor.class)
                    .from(BASE_IMAGE)
                    .exportAsString();
            return ShrinkWrap.create(GenericArchive.class)
                    .add(new StringAsset(dockerDescriptor), "Dockerfile");
        }
    }

    @Cube("containerWithAnnotatedLink")
    public static class TestContainerObjectWithAnnotatedLink {

        @CubeDockerFile
        public static Archive<?> createDockerfile() {
            String dockerDescriptor = Descriptors.create(DockerDescriptor.class)
                    .from(BASE_IMAGE)
                    .exportAsString();
            return ShrinkWrap.create(GenericArchive.class)
                    .add(new StringAsset(dockerDescriptor), "Dockerfile");
        }

        @Cube("inner")
        @Link("db:db")
        TestContainerObjectDefinedUsingDescriptor linkedContainerObject;
    }

    @Cube("containerWithNonAnnotatedLink")
    public static class TestContainerObjectWithNonAnnotatedLink {

        @CubeDockerFile
        public static Archive<?> createDockerfile() {
            String dockerDescriptor = Descriptors.create(DockerDescriptor.class)
                    .from(BASE_IMAGE)
                    .exportAsString();
            return ShrinkWrap.create(GenericArchive.class)
                    .add(new StringAsset(dockerDescriptor), "Dockerfile");
        }

        @Cube("inner")
        TestContainerObjectDefinedUsingDescriptor linkedContainerObject;
    }

    @Cube("containerDefinedUsingImage")
    @Image(BASE_IMAGE)
    public static class TestContainerObjectDefinedUsingImage {
    }

    @Cube("containerWithEnvironmentVariables")
    @Image(BASE_IMAGE)
    @Environment(key = "a", value = "b")
    @Environment(key = "c",  value = "d")
    public static class TestContainerObjectDefinedUsingImageAndEnvironmentVariables {
    }

    @Cube("containerWithVolumes")
    @Image(BASE_IMAGE)
    @Volume(hostPath = "/mypath", containerPath = "/containerPath")
    @Volume(hostPath = "/mypath2", containerPath = "/containerPath2")
    public static class TestContainerObjectDefinedUsingImageAndVolumes {
    }

    @Cube("containerWithCubeIp")
    @Image(BASE_IMAGE)
    public static class TestContainerObjectWithCubeIp {

        @CubeIp
        String cubeIp;
    }

    @Cube(value = "containerWithHostPort", portBinding = "8080->8080/tcp")
    @Image(BASE_IMAGE)
    public static class TestContainerObjectWithHostPort {

        @HostPort(8080)
        int port;
    }

    //</editor-fold>
    //<editor-fold desc="utility methods used by test methods">

    private static void deleteTestDirectory() {
        File tempDirectory = new File(System.getProperty("java.io.tmpdir"));
        final File[] testsDirectories = tempDirectory.listFiles(DockerContainerObjectBuilderTest::testDirectoryFilter);
        for (File testDirectory: testsDirectories) {
            try {
                FileUtils.deleteDirectory(testDirectory);
            } catch (IOException e) {
                // ignore
            }
        }
    }

    private static File findGeneratedDirectory() {
        File tempDirectory = new File(System.getProperty("java.io.tmpdir"));
        final File[] testsDirectories = tempDirectory.listFiles(DockerContainerObjectBuilderTest::testDirectoryFilter);
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

    private static Void objectContainerEnricherMockEnrich(InvocationOnMock invocation) throws Throwable {
        // simulate ContainerObjectTestEnricher by setting every field annotated with @Cube with a new instance
        Object containerObject = invocation.getArguments()[0];
        ReflectionUtil.getFieldsWithAnnotation(containerObject.getClass(), Cube.class)
                .stream().forEach(field -> {
            try {
                field.set(containerObject, field.getType().newInstance());
            } catch (IllegalAccessException | InstantiationException e) {
                throw new RuntimeException(e);
            }
        });
        return null;
    }

    private static void initDockerCubeInternalIP(DockerCube dockerCube, String internalIP) {
        PrivilegedExceptionAction<Void> action = () -> {
            Field dockerCubePortBindingsField = DockerCube.class.getDeclaredField("portBindings");
            dockerCubePortBindingsField.setAccessible(true);
            Class<?> dockerCubePortBindingsClass = dockerCubePortBindingsField.getType();
            Field dockerCubePortBindingsInternalIpField = dockerCubePortBindingsClass.getDeclaredField("internalIP");
            dockerCubePortBindingsInternalIpField.setAccessible(true);
            Object dockerCubePortBindings = dockerCubePortBindingsField.get(dockerCube);
            dockerCubePortBindingsInternalIpField.set(dockerCubePortBindings, internalIP);
            return null;
        };
        try {
            AccessController.doPrivileged(action);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void initDockerCubeMappedPort(DockerCube dockerCube, String containerIP, int exposedPort, int boundPort) {
        PrivilegedExceptionAction<Void> action = () -> {
            Field dockerCubePortBindingsField = DockerCube.class.getDeclaredField("portBindings");
            dockerCubePortBindingsField.setAccessible(true);
            Class<?> dockerCubePortBindingsClass = dockerCubePortBindingsField.getType();
            Field dockerCubePortBindingsMappedPortsField = dockerCubePortBindingsClass.getDeclaredField("mappedPorts");
            dockerCubePortBindingsMappedPortsField.setAccessible(true);
            Object dockerCubePortBindings = dockerCubePortBindingsField.get(dockerCube);
            Map<Integer, HasPortBindings.PortAddress> mappedPorts = (Map<Integer, HasPortBindings.PortAddress>) dockerCubePortBindingsMappedPortsField.get(dockerCubePortBindings);
            mappedPorts.put(exposedPort, new HasPortBindings.PortAddressImpl(containerIP, boundPort));
            return null;
        };
        try {
            AccessController.doPrivileged(action);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //</editor-fold>
}
