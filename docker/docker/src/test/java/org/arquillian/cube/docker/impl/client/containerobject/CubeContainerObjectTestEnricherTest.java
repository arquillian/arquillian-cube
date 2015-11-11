package org.arquillian.cube.docker.impl.client.containerobject;


import org.arquillian.cube.CubeController;
import org.arquillian.cube.containerobject.*;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.docker.impl.model.DockerCube;
import org.arquillian.cube.impl.model.LocalCubeRegistry;
import org.arquillian.cube.spi.CubeRegistry;
import org.jboss.arquillian.core.spi.ServiceLoader;
import org.hamcrest.CoreMatchers;
import org.hamcrest.core.Is;
import org.jboss.arquillian.container.spi.ContainerRegistry;
import org.jboss.arquillian.core.api.Injector;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.GenericArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.docker.DockerDescriptor;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Set;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.isNotNull;
import static org.mockito.Mockito.*;

public class CubeContainerObjectTestEnricherTest {

    private CubeRegistry cubeRegistry = new LocalCubeRegistry();
    private CubeController cubeController;
    private DockerClientExecutor dockerClientExecutor;
    private Injector injector;
    private ServiceLoader serviceLoader;

    @AfterClass
    public static void cleanEnvironment() {
        deleteTestDirectory();
    }

    @Before
    public void init() {
        cubeController = mock(CubeController.class);
        dockerClientExecutor = mock(DockerClientExecutor.class);
        injector = mock(Injector.class);
        serviceLoader = mock(ServiceLoader.class);
        when(serviceLoader.all(any(Class.class))).thenReturn(new ArrayList<Object>());

        //We asure that there is no previous executions there.
        deleteTestDirectory();
    }

    @Test
    public void shouldStartAContainerObject() {
        CubeContainerObjectTestEnricher cubeContainerObjectTestEnricher = new CubeContainerObjectTestEnricher();
        cubeContainerObjectTestEnricher.injectorInstance = new Instance<Injector>() {
            @Override
            public Injector get() {
                return injector;
            }
        };
        cubeContainerObjectTestEnricher.cubeRegistryInstance = new Instance<CubeRegistry>() {
            @Override
            public CubeRegistry get() {
                return cubeRegistry;
            }
        };
        cubeContainerObjectTestEnricher.serviceLoader = new Instance<ServiceLoader>() {
            @Override
            public ServiceLoader get() {
                return serviceLoader;
            }
        };
        cubeContainerObjectTestEnricher.cubeControllerInstance = new Instance<CubeController>() {
            @Override
            public CubeController get() {
                return cubeController;
            }
        };
        cubeContainerObjectTestEnricher.dockerClientExecutorInstance = new Instance<DockerClientExecutor>() {
            @Override
            public DockerClientExecutor get() {
                return dockerClientExecutor;
            }
        };

        InjectableTest injectableTest = new InjectableTest();
        cubeContainerObjectTestEnricher.enrich(injectableTest);

        final org.arquillian.cube.spi.Cube mycontainer = cubeRegistry.getCube("mycontainer");
        assertThat(mycontainer, is(notNullValue()));
        assertThat(mycontainer.hasMetadata(IsContainerObject.class), is(true));
        assertThat(mycontainer.getMetadata(IsContainerObject.class).getTestClass().getName(), is(InjectableTest.class.getName()));

        verify(cubeController, times(1)).start("mycontainer");
        verify(cubeController, times(1)).create("mycontainer");

    }

    @Test
    public void shouldShouldStartAContainerObjectDefinedUsingDescriptor() {
        CubeContainerObjectTestEnricher cubeContainerObjectTestEnricher = new CubeContainerObjectTestEnricher();
        cubeContainerObjectTestEnricher.injectorInstance = new Instance<Injector>() {
            @Override
            public Injector get() {
                return injector;
            }
        };
        cubeContainerObjectTestEnricher.cubeRegistryInstance = new Instance<CubeRegistry>() {
            @Override
            public CubeRegistry get() {
                return cubeRegistry;
            }
        };
        cubeContainerObjectTestEnricher.serviceLoader = new Instance<ServiceLoader>() {
            @Override
            public ServiceLoader get() {
                return serviceLoader;
            }
        };
        cubeContainerObjectTestEnricher.cubeControllerInstance = new Instance<CubeController>() {
            @Override
            public CubeController get() {
                return cubeController;
            }
        };
        cubeContainerObjectTestEnricher.dockerClientExecutorInstance = new Instance<DockerClientExecutor>() {
            @Override
            public DockerClientExecutor get() {
                return dockerClientExecutor;
            }
        };

        SecondInjectableTest secondInjectableTest = new SecondInjectableTest();
        cubeContainerObjectTestEnricher.enrich(secondInjectableTest);

        final org.arquillian.cube.spi.Cube mycontainer = cubeRegistry.getCube("mycontainer2");
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
        cubeContainerObjectTestEnricher.injectorInstance = new Instance<Injector>() {
            @Override
            public Injector get() {
                return injector;
            }
        };
        cubeContainerObjectTestEnricher.cubeRegistryInstance = new Instance<CubeRegistry>() {
            @Override
            public CubeRegistry get() {
                return cubeRegistry;
            }
        };
        cubeContainerObjectTestEnricher.serviceLoader = new Instance<ServiceLoader>() {
            @Override
            public ServiceLoader get() {
                return serviceLoader;
            }
        };
        cubeContainerObjectTestEnricher.cubeControllerInstance = new Instance<CubeController>() {
            @Override
            public CubeController get() {
                return cubeController;
            }
        };
        cubeContainerObjectTestEnricher.dockerClientExecutorInstance = new Instance<DockerClientExecutor>() {
            @Override
            public DockerClientExecutor get() {
                return dockerClientExecutor;
            }
        };

        ThirdInjetableTest thirdInjetableTest = new ThirdInjetableTest();
        cubeContainerObjectTestEnricher.enrich(thirdInjetableTest);

        final org.arquillian.cube.spi.Cube outterContainer = cubeRegistry.getCube("outter");
        assertThat(outterContainer, is(notNullValue()));

        DockerCube dockerCube = (DockerCube) outterContainer;
        final Set<String> links = (Set<String>) dockerCube.configuration().get(DockerClientExecutor.LINKS);
        assertThat(links.size(), is(1));
        assertThat(links, hasItem("db:db"));

        final org.arquillian.cube.spi.Cube innerContainer = cubeRegistry.getCube("inner");
        assertThat(innerContainer, is(notNullValue()));

        verify(cubeController, times(1)).start("outter");
        verify(cubeController, times(1)).create("outter");

        verify(cubeController, times(1)).start("inner");
        verify(cubeController, times(1)).create("inner");
    }

    @Test
    public void shouldLinkInnerContainersWithoutLink() {
        CubeContainerObjectTestEnricher cubeContainerObjectTestEnricher = new CubeContainerObjectTestEnricher();
        cubeContainerObjectTestEnricher.injectorInstance = new Instance<Injector>() {
            @Override
            public Injector get() {
                return injector;
            }
        };
        cubeContainerObjectTestEnricher.cubeRegistryInstance = new Instance<CubeRegistry>() {
            @Override
            public CubeRegistry get() {
                return cubeRegistry;
            }
        };
        cubeContainerObjectTestEnricher.serviceLoader = new Instance<ServiceLoader>() {
            @Override
            public ServiceLoader get() {
                return serviceLoader;
            }
        };
        cubeContainerObjectTestEnricher.cubeControllerInstance = new Instance<CubeController>() {
            @Override
            public CubeController get() {
                return cubeController;
            }
        };
        cubeContainerObjectTestEnricher.dockerClientExecutorInstance = new Instance<DockerClientExecutor>() {
            @Override
            public DockerClientExecutor get() {
                return dockerClientExecutor;
            }
        };

        FifthInjetableTest fifthInjetableTest = new FifthInjetableTest();
        cubeContainerObjectTestEnricher.enrich(fifthInjetableTest);

        final org.arquillian.cube.spi.Cube outterContainer = cubeRegistry.getCube("outter");
        assertThat(outterContainer, is(notNullValue()));

        DockerCube dockerCube = (DockerCube) outterContainer;
        final Set<String> links = (Set<String>) dockerCube.configuration().get(DockerClientExecutor.LINKS);
        assertThat(links.size(), is(1));
        assertThat(links, hasItem("inner:inner"));

        final org.arquillian.cube.spi.Cube innerContainer = cubeRegistry.getCube("inner");
        assertThat(innerContainer, is(notNullValue()));

        verify(cubeController, times(1)).start("outter");
        verify(cubeController, times(1)).create("outter");

        verify(cubeController, times(1)).start("inner");
        verify(cubeController, times(1)).create("inner");
    }

    @Test
    public void shouldStartAContainerObjectDefinedAsImage() {
        CubeContainerObjectTestEnricher cubeContainerObjectTestEnricher = new CubeContainerObjectTestEnricher();
        cubeContainerObjectTestEnricher.injectorInstance = new Instance<Injector>() {
            @Override
            public Injector get() {
                return injector;
            }
        };
        cubeContainerObjectTestEnricher.cubeRegistryInstance = new Instance<CubeRegistry>() {
            @Override
            public CubeRegistry get() {
                return cubeRegistry;
            }
        };
        cubeContainerObjectTestEnricher.serviceLoader = new Instance<ServiceLoader>() {
            @Override
            public ServiceLoader get() {
                return serviceLoader;
            }
        };
        cubeContainerObjectTestEnricher.cubeControllerInstance = new Instance<CubeController>() {
            @Override
            public CubeController get() {
                return cubeController;
            }
        };
        cubeContainerObjectTestEnricher.dockerClientExecutorInstance = new Instance<DockerClientExecutor>() {
            @Override
            public DockerClientExecutor get() {
                return dockerClientExecutor;
            }
        };

        FourthInjectableTest injectableTest = new FourthInjectableTest();
        cubeContainerObjectTestEnricher.enrich(injectableTest);

        final org.arquillian.cube.spi.Cube image = cubeRegistry.getCube("image");
        assertThat(image, is(notNullValue()));
        assertThat(image.hasMetadata(IsContainerObject.class), is(true));
        assertThat(image.getMetadata(IsContainerObject.class).getTestClass().getName(), is(FourthInjectableTest.class.getName()));

        verify(cubeController, times(1)).start("image");
        verify(cubeController, times(1)).create("image");

        DockerCube dockerCube = (DockerCube) image;
        assertThat((String)dockerCube.configuration().get(DockerClientExecutor.IMAGE), is("tomee:8-jre-1.7.2-webprofile"));

    }

    private static class InjectableTest {
        @Cube("mycontainer")
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

    public static class TestLinkContainerObject {
        @CubeDockerFile
        public static Archive<?> createDockerfile() {
            String dockerDescriptor = Descriptors.create(DockerDescriptor.class).from("mysql").exportAsString();
            return ShrinkWrap.create(GenericArchive.class)
                    .add(new StringAsset(dockerDescriptor), "Dockerfile");
        }
    }

    private static void deleteTestDirectory() {
        File tempDirectory = new File(System.getProperty("java.io.tmpdir"));
        final File[] tests = tempDirectory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith("Test");
            }
        });
        for (File testDirectory : tests) {
            testDirectory.delete();
        }
    }

    private File findGeneratedDirectory() {

        File tempDirectory = new File(System.getProperty("java.io.tmpdir"));
        final File[] tests = tempDirectory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith("Test");
            }
        });
        if (tests.length > 0) {
            return tests[0];
        } else {
            return null;
        }
    }
}
