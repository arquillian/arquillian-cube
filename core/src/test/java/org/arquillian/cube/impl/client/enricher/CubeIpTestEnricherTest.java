package org.arquillian.cube.impl.client.enricher;

import org.arquillian.cube.CubeIp;
import org.arquillian.cube.docker.impl.requirement.RequiresDocker;
import org.arquillian.cube.docker.impl.requirement.RequiresDockerMachine;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.metadata.HasPortBindings;
import org.hamcrest.core.Is;
import org.jboss.arquillian.core.api.Instance;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@Category({RequiresDocker.class, RequiresDockerMachine.class})
public class CubeIpTestEnricherTest {

    @Test
    public void should_enrich_test_with_container_ip() {
        CubeIpTestEnricher cubeIpTestEnricher = new CubeIpTestEnricher();
        cubeIpTestEnricher.cubeRegistryInstance = new Instance<CubeRegistry>() {
            @Override
            public CubeRegistry get() {

                final CubeRegistry cubeRegistry = Mockito.mock(CubeRegistry.class);
                Mockito.when(cubeRegistry.getCube("test")).thenAnswer(new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                        final HasPortBindings hasPortBindings = Mockito.mock(HasPortBindings.class);
                        Mockito.when(hasPortBindings.getInternalIP()).thenReturn("192.168.99.100");
                        Mockito.when(hasPortBindings.getContainerIP()).thenReturn("192.168.99.101");
                        final Cube<?> cube = Mockito.mock(Cube.class);
                        Mockito.when(cube.hasMetadata(HasPortBindings.class)).thenReturn(true);
                        Mockito.when(cube.getMetadata(HasPortBindings.class)).thenReturn(hasPortBindings);
                        return cube;
                    }
                });
                return cubeRegistry;
            }
        };

        final CubeIpTestEnricherTest.MyTestExternal testCase = new CubeIpTestEnricherTest.MyTestExternal();
        cubeIpTestEnricher.enrich(testCase);
        assertThat(testCase.ip, is("192.168.99.101"));
    }

    @Test
    public void should_enrich_test_with_internal_ip() {
        CubeIpTestEnricher cubeIpTestEnricher = new CubeIpTestEnricher();
        cubeIpTestEnricher.cubeRegistryInstance = new Instance<CubeRegistry>() {
            @Override
            public CubeRegistry get() {

                final CubeRegistry cubeRegistry = Mockito.mock(CubeRegistry.class);
                Mockito.when(cubeRegistry.getCube("test")).thenAnswer(new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                        final HasPortBindings hasPortBindings = Mockito.mock(HasPortBindings.class);
                        Mockito.when(hasPortBindings.getInternalIP()).thenReturn("192.168.99.100");
                        final Cube<?> cube = Mockito.mock(Cube.class);
                        Mockito.when(cube.hasMetadata(HasPortBindings.class)).thenReturn(true);
                        Mockito.when(cube.getMetadata(HasPortBindings.class)).thenReturn(hasPortBindings);
                        return cube;
                    }
                });
                return cubeRegistry;
            }
        };

        final CubeIpTestEnricherTest.MyTest testCase = new CubeIpTestEnricherTest.MyTest();
        cubeIpTestEnricher.enrich(testCase);
        assertThat(testCase.ip, is("192.168.99.100"));
    }

    @Test
    public void should_enrich_test_method_with_internal() throws NoSuchMethodException {
        CubeIpTestEnricher cubeIpTestEnricher = new CubeIpTestEnricher();
        cubeIpTestEnricher.cubeRegistryInstance = new Instance<CubeRegistry>() {
            @Override
            public CubeRegistry get() {

                final CubeRegistry cubeRegistry = Mockito.mock(CubeRegistry.class);
                Mockito.when(cubeRegistry.getCube("test")).thenAnswer(new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                        final HasPortBindings hasPortBindings = Mockito.mock(HasPortBindings.class);
                        Mockito.when(hasPortBindings.getInternalIP()).thenReturn("192.168.99.100");
                        final Cube<?> cube = Mockito.mock(Cube.class);
                        Mockito.when(cube.hasMetadata(HasPortBindings.class)).thenReturn(true);
                        Mockito.when(cube.getMetadata(HasPortBindings.class)).thenReturn(hasPortBindings);
                        return cube;
                    }
                });
                return cubeRegistry;
            }
        };

        final CubeIpTestEnricherTest.MyTest testCase = new CubeIpTestEnricherTest.MyTest();
        final Object[] myMethods =
            cubeIpTestEnricher.resolve(testCase.getClass().getMethod("myMethod", String.class, String.class));
        assertThat((String) myMethods[1], Is.is("192.168.99.100"));
    }

    public static class MyTest {
        @CubeIp(containerName = "test")
        String ip;

        public void myMethod(String first, @CubeIp(containerName = "test") String ip) {

        }
    }

    public static class MyTestExternal {
        @CubeIp(containerName = "test", internal = false)
        String ip;
    }
}
