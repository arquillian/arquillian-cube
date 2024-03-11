package org.arquillian.cube.istio.impl;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import org.arquillian.cube.istio.api.RestoreIstioResource;
import org.jboss.arquillian.test.spi.event.suite.AfterClass;
import org.jboss.arquillian.test.spi.event.suite.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IstioResourcesApplierTest {

    @Mock
    private IstioClientAdapter istioClientAdapter;

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
        when(istioClientAdapter.registerCustomResources(any(InputStream.class)))
            .thenReturn(Arrays.asList(istioResource));

        // When

        istioResourceApplier.applyIstioResourcesAtClassScope(beforeClass, istioClientAdapter);

        // Then

        verify(istioClientAdapter, times(1)).registerCustomResources(any(InputStream.class));
        assertThat(istioResourceApplier.getResourcesMap())
            .hasSize(1)
            .containsValue(Collections.singletonList(istioResource));
        assertThat(istioResourceApplier.getRestoredResourcesMap())
            .hasSize(0);
    }

    @Test
    public void should_apply_tests_with_istio_resources_and_restore() {

        // Given
        final BeforeClass beforeClass = new BeforeClass(TestWithIstioResourceAndRestore.class);
        final IstioResourcesApplier istioResourceApplier = createIstioResourceApplier();
        when(istioClientAdapter.registerCustomResources(any(InputStream.class)))
            .thenReturn(Arrays.asList(istioResource));

        // When

        istioResourceApplier.applyIstioResourcesAtClassScope(beforeClass, istioClientAdapter);

        // Then

        verify(istioClientAdapter, times(1)).registerCustomResources(any(InputStream.class));
        assertThat(istioResourceApplier.getResourcesMap())
            .isNotNull()
            .containsValue(Arrays.asList(istioResource));
        assertThat(istioResourceApplier.getRestoredResourcesMap())
            .hasSize(0);
    }

    @Test
    public void should_delete_registered_istio_resources() {

        // Given
        final BeforeClass beforeClass = new BeforeClass(TestWithIstioResource.class);
        final IstioResourcesApplier istioResourceApplier = createIstioResourceApplier();
        when(istioClientAdapter.registerCustomResources(any(InputStream.class)))
            .thenReturn(Arrays.asList(istioResource));
        istioResourceApplier.applyIstioResourcesAtClassScope(beforeClass, istioClientAdapter);


        // When

        istioResourceApplier.removeIstioResourcesAtClassScope(new AfterClass(TestWithIstioResource.class), istioClientAdapter);

        // Then

        verify(istioClientAdapter, times(1)).unregisterCustomResource(istioResource);
    }

    @Test
    public void should_delete_registered_istio_resources_and_restore() {

        // Given
        final BeforeClass beforeClass = new BeforeClass(TestWithIstioResourceAndRestore.class);
        final IstioResourcesApplier istioResourceApplier = createIstioResourceApplier();
        when(istioClientAdapter.registerCustomResources(any(InputStream.class)))
            .thenReturn(Arrays.asList(istioResource));
        istioResourceApplier.applyIstioResourcesAtClassScope(beforeClass, istioClientAdapter);

        // When

        istioResourceApplier.removeIstioResourcesAtClassScope(new AfterClass(TestWithIstioResourceAndRestore.class), istioClientAdapter);

        // Then

        verify(istioClientAdapter, times(2)).registerCustomResources(any(InputStream.class));
        verify(istioClientAdapter, times(0)).unregisterCustomResource(istioResource);
    }

    @Test
    public void should_delete_registered_istio_resources_and_not_restore_if_resource_is_different() {

        // Given
        final BeforeClass beforeClass = new BeforeClass(TestWithIstioResourceAndRestore.class);
        final IstioResourcesApplier istioResourceApplier = createIstioResourceApplier();
        when(istioClientAdapter.registerCustomResources(any(InputStream.class)))
            .thenReturn(Arrays.asList(istioResource), Arrays.asList(istioResource2));
        istioResourceApplier.applyIstioResourcesAtClassScope(beforeClass, istioClientAdapter);

        // When

        istioResourceApplier.removeIstioResourcesAtClassScope(new AfterClass(TestWithIstioResourceAndRestore.class), istioClientAdapter);

        // Then

        verify(istioClientAdapter, times(2)).registerCustomResources(any(InputStream.class));
        verify(istioClientAdapter, times(1)).unregisterCustomResource(istioResource);
    }

    private IstioResourcesApplier createIstioResourceApplier() {

        when(istioResource.getMetadata()).thenReturn(meta);
        when(meta.getName()).thenReturn("recommendation");
        when(meta.getNamespace()).thenReturn("tutorial");

        when(istioResource2.getMetadata()).thenReturn(meta2);
        when(meta2.getName()).thenReturn("different");
        lenient().when(meta2.getNamespace()).thenReturn("tutorial");

        when(istioClientAdapter.unregisterCustomResource(any(IstioResource.class)))
            .thenReturn(true);

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
