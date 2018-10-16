package org.arquillian.cube.istio.impl;

import io.fabric8.kubernetes.api.model.v4_0.ObjectMeta;
import java.io.InputStream;
import java.util.Arrays;
import me.snowdrop.istio.api.IstioResource;
import me.snowdrop.istio.client.IstioClient;
import org.arquillian.cube.istio.api.RestoreIstioResource;
import org.jboss.arquillian.test.spi.event.suite.AfterClass;
import org.jboss.arquillian.test.spi.event.suite.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IstioResourcesApplierTest {

    @Mock
    private IstioClient istioClient;

    @Mock
    private IstioResource istioResource;

    @Mock
    private IstioResource istioResource2;

    @Mock
    private ObjectMeta meta;

    @Mock
    private ObjectMeta meta2;

    @Test
    public void should_apply_tests_with_istio_resources() {

        // Given
        final BeforeClass beforeClass = new BeforeClass(TestWithIstioResource.class);
        final IstioResourcesApplier istioResourceApplier = createIstioResourceApplier();
        when(istioClient.registerCustomResources(any(InputStream.class)))
            .thenReturn(Arrays.asList(istioResource));

        // When

        istioResourceApplier.applyIstioResourcesAtClassScope(beforeClass, istioClient);

        // Then

        verify(istioClient, times(1)).registerCustomResources(any(InputStream.class));
        assertThat(istioResourceApplier.getResourcesMap())
            .hasSize(1)
            .containsValue(Arrays.asList(istioResource));
        assertThat(istioResourceApplier.getRestoredResourcesMap())
            .hasSize(0);
    }

    @Test
    public void should_apply_tests_with_istio_resources_and_restore() {

        // Given
        final BeforeClass beforeClass = new BeforeClass(TestWithIstioResourceAndRestore.class);
        final IstioResourcesApplier istioResourceApplier = createIstioResourceApplier();
        when(istioClient.registerCustomResources(any(InputStream.class)))
            .thenReturn(Arrays.asList(istioResource));

        // When

        istioResourceApplier.applyIstioResourcesAtClassScope(beforeClass, istioClient);

        // Then

        verify(istioClient, times(1)).registerCustomResources(any(InputStream.class));
        assertThat(istioResourceApplier.getResourcesMap())
            .hasSize(1)
            .containsValue(Arrays.asList(istioResource));
        assertThat(istioResourceApplier.getRestoredResourcesMap())
            .hasSize(0);
    }

    @Test
    public void should_delete_registered_istio_resources() {

        // Given
        final BeforeClass beforeClass = new BeforeClass(TestWithIstioResource.class);
        final IstioResourcesApplier istioResourceApplier = createIstioResourceApplier();
        when(istioClient.registerCustomResources(any(InputStream.class)))
            .thenReturn(Arrays.asList(istioResource));
        istioResourceApplier.applyIstioResourcesAtClassScope(beforeClass, istioClient);


        // When

        istioResourceApplier.removeIstioResourcesAtClassScope(new AfterClass(TestWithIstioResource.class), istioClient);

        // Then

        verify(istioClient, times(1)).unregisterCustomResource(istioResource);
    }

    @Test
    public void should_delete_registered_istio_resources_and_restore() {

        // Given
        final BeforeClass beforeClass = new BeforeClass(TestWithIstioResourceAndRestore.class);
        final IstioResourcesApplier istioResourceApplier = createIstioResourceApplier();
        when(istioClient.registerCustomResources(any(InputStream.class)))
            .thenReturn(Arrays.asList(istioResource));
        istioResourceApplier.applyIstioResourcesAtClassScope(beforeClass, istioClient);

        // When

        istioResourceApplier.removeIstioResourcesAtClassScope(new AfterClass(TestWithIstioResourceAndRestore.class), istioClient);

        // Then

        verify(istioClient, times(2)).registerCustomResources(any(InputStream.class));
        verify(istioClient, times(0)).unregisterCustomResource(istioResource);
    }

    @Test
    public void should_delete_registered_istio_resources_and_not_restore_if_resource_is_different() {

        // Given
        final BeforeClass beforeClass = new BeforeClass(TestWithIstioResourceAndRestore.class);
        final IstioResourcesApplier istioResourceApplier = createIstioResourceApplier();
        when(istioClient.registerCustomResources(any(InputStream.class)))
            .thenReturn(Arrays.asList(istioResource), Arrays.asList(istioResource2));
        istioResourceApplier.applyIstioResourcesAtClassScope(beforeClass, istioClient);

        // When

        istioResourceApplier.removeIstioResourcesAtClassScope(new AfterClass(TestWithIstioResourceAndRestore.class), istioClient);

        // Then

        verify(istioClient, times(2)).registerCustomResources(any(InputStream.class));
        verify(istioClient, times(1)).unregisterCustomResource(istioResource);
    }

    private IstioResourcesApplier createIstioResourceApplier() {

        when(istioResource.getMetadata()).thenReturn(meta);
        when(meta.getName()).thenReturn("recommendation");
        when(meta.getNamespace()).thenReturn("tutorial");

        when(istioResource2.getMetadata()).thenReturn(meta2);
        when(meta2.getName()).thenReturn("different");
        when(meta2.getNamespace()).thenReturn("tutorial");

        when(istioClient.registerCustomResources(any(InputStream.class)))
            .thenReturn(Arrays.asList(istioResource), Arrays.asList(istioResource2));

        return new IstioResourcesApplier();
    }

    @org.arquillian.cube.istio.api.IstioResource("classpath:virtual-service.yml")
    class TestWithIstioResource {
    }

    @org.arquillian.cube.istio.api.IstioResource("classpath:virtual-service.yml")
    @RestoreIstioResource("classpath:virtual-service.yml")
    class TestWithIstioResourceAndRestore {
    }
}
