package org.arquillian.cube.istio.impl;

import io.fabric8.istio.client.IstioClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IstioClientAdapterTest {
    @Mock
    Resource<IstioResource> loadedResource;
    @Mock
    MixedOperation<IstioResource, IstioResourceList, Resource<IstioResource>> resources;
    @Mock
    private IstioClient istioClient;

    @Before
    public void setup_mock_expectations() {
        when(istioClient.resources(IstioResource.class, IstioResourceList.class)).thenReturn(resources);
        when(resources.load(any(InputStream.class))).thenReturn(loadedResource);
        when(resources.load(any(String.class))).thenReturn(loadedResource);
        when(resources.resource(any(IstioResource.class))).thenReturn(loadedResource);
        when(loadedResource.create()).thenReturn(new IstioResource());
        when(loadedResource.delete()).thenReturn(Collections.emptyList());
    }

    @Test
    public void should_deploy_from_input_stream() throws IOException {

        // given
        final IstioClientAdapter istioClientAdapter = new IstioClientAdapter(istioClient);

        // when
        final URL resource =
            Thread.currentThread().getContextClassLoader().getResource("route-rule-reviews-test-v2.yaml");
        try (InputStream is = resource.openStream()) {
            final List<IstioResource> istioResources = istioClientAdapter.registerCustomResources(is);

            // then
            assertThat(istioResources).isNotNull();
            assertThat(istioResources).isNotEmpty();
            assertThat(istioResources).hasSize(1);
        }
    }

    @Test
    public void should_deploy_from_string() throws IOException {

        // given
        final IstioClientAdapter istioClientAdapter = new IstioClientAdapter(istioClient);

        // when
        final URL resource =
            Thread.currentThread().getContextClassLoader().getResource("route-rule-reviews-test-v2.yaml");
        try (InputStream in = resource.openStream()) {
            byte[] bytes = in.readAllBytes();
            final List<IstioResource> istioResources = istioClientAdapter.registerCustomResources(new String(bytes, StandardCharsets.UTF_8));

            // then
            assertThat(istioResources).isNotNull();
            assertThat(istioResources).isNotEmpty();
            assertThat(istioResources).hasSize(1);
        }
    }

    @Test
    public void should_undeploy() throws IOException {

        // given
        final IstioClientAdapter istioClientAdapter = new IstioClientAdapter(istioClient);

        // when
        final URL resource =
            Thread.currentThread().getContextClassLoader().getResource("route-rule-reviews-test-v2.yaml");
        try (InputStream is = resource.openStream()) {
            final List<IstioResource> istioResources = istioClientAdapter.registerCustomResources(is);

            Boolean result = istioClientAdapter.unregisterCustomResource(istioResources.get(0));

            // then
            assertThat(result).isTrue();
        }
    }

    @Test
    public void should_unwrap() {
        // given
        final IstioClientAdapter istioClientAdapter = new IstioClientAdapter(istioClient);
        // when
        final IstioClient unwrapped = istioClientAdapter.unwrap();
        // then
        assertThat(unwrapped).isNotNull();
        assertThat(unwrapped).isInstanceOf(IstioClient.class);
    }
}
