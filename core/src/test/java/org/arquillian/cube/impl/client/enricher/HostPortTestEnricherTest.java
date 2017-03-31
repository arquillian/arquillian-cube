package org.arquillian.cube.impl.client.enricher;

import org.arquillian.cube.HostPort;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.metadata.HasPortBindings;
import org.hamcrest.CoreMatchers;
import org.jboss.arquillian.core.api.Instance;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.junit.Assert.assertThat;

public class HostPortTestEnricherTest {

    @Test
    public void shouldEnrichTest() {
        HostPortTestEnricher hostPortTestEnricher = new HostPortTestEnricher();
        hostPortTestEnricher.cubeRegistryInstance = new Instance<CubeRegistry>() {
            @Override
            public CubeRegistry get() {

                final CubeRegistry cubeRegistry = Mockito.mock(CubeRegistry.class);
                Mockito.when(cubeRegistry.getCube("test")).thenAnswer(new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                        final HasPortBindings hasPortBindings = Mockito.mock(HasPortBindings.class);
                        Mockito.when(hasPortBindings.getMappedAddress(99))
                            .thenReturn(new HasPortBindings.PortAddressImpl("192.168.99.100", 9999));
                        final Cube<?> cube = Mockito.mock(Cube.class);
                        Mockito.when(cube.getMetadata(HasPortBindings.class)).thenReturn(hasPortBindings);
                        return cube;
                    }
                });
                return cubeRegistry;
            }
        };
        final MyTest testCase = new MyTest();
        hostPortTestEnricher.enrich(testCase);
        Assert.assertThat(testCase.port, CoreMatchers.is(9999));
    }

    @Test
    public void shouldEnrichTestMethod() throws NoSuchMethodException {
        HostPortTestEnricher hostPortTestEnricher = new HostPortTestEnricher();
        hostPortTestEnricher.cubeRegistryInstance = new Instance<CubeRegistry>() {
            @Override
            public CubeRegistry get() {

                final CubeRegistry cubeRegistry = Mockito.mock(CubeRegistry.class);
                Mockito.when(cubeRegistry.getCube("test")).thenAnswer(new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                        final HasPortBindings hasPortBindings = Mockito.mock(HasPortBindings.class);
                        Mockito.when(hasPortBindings.getMappedAddress(99))
                            .thenReturn(new HasPortBindings.PortAddressImpl("192.168.99.100", 9999));
                        final Cube<?> cube = Mockito.mock(Cube.class);
                        Mockito.when(cube.getMetadata(HasPortBindings.class)).thenReturn(hasPortBindings);
                        return cube;
                    }
                });
                return cubeRegistry;
            }
        };
        MyTest test = new MyTest();
        Object[] myMethods = hostPortTestEnricher.resolve(test.getClass().getMethod("myMethod", String.class, int.class));
        assertThat((int) myMethods[1], is(9999));
    }

    @Test
    public void shouldNotEnrichUnknownContainers() throws NoSuchMethodException {
        HostPortTestEnricher hostPortTestEnricher = new HostPortTestEnricher();
        hostPortTestEnricher.cubeRegistryInstance = new Instance<CubeRegistry>() {
            @Override
            public CubeRegistry get() {

                final CubeRegistry cubeRegistry = Mockito.mock(CubeRegistry.class);
                Mockito.when(cubeRegistry.getCube("test2")).thenAnswer(new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                        final HasPortBindings hasPortBindings = Mockito.mock(HasPortBindings.class);
                        Mockito.when(hasPortBindings.getMappedAddress(99))
                            .thenReturn(new HasPortBindings.PortAddressImpl("192.168.99.100", 9999));
                        final Cube<?> cube = Mockito.mock(Cube.class);
                        Mockito.when(cube.getMetadata(HasPortBindings.class)).thenReturn(hasPortBindings);
                        return cube;
                    }
                });
                return cubeRegistry;
            }
        };
        final MyTest testCase = new MyTest();
        hostPortTestEnricher.enrich(testCase);
        Assert.assertThat(testCase.port, CoreMatchers.is(0));
    }

    public static class MyTest {
        @HostPort(containerName = "test", value = 99)
        int port;

        public void myMethod(String first, @HostPort(containerName = "test", value = 99) int port) {

        }
    }
}
