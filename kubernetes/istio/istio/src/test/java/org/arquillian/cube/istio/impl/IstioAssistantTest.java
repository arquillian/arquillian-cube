package org.arquillian.cube.istio.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import io.fabric8.istio.client.IstioClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.arquillian.cube.istio.api.IstioResource;

@RunWith(MockitoJUnitRunner.class)
public class IstioAssistantTest {

    @Mock
    private IstioClient istioClient;

    @Mock
    private IstioResource istioResource;

    @Before
    public void setup_mock_expectations() {
//        when(istioClient.registerCustomResources(any(InputStream.class)))
//            .thenReturn(Arrays.asList(istioResource));
    }

    @Test
    public void should_load_route_from_url() throws IOException {

        // given
        final IstioAssistant istioAssistant = new IstioAssistant(istioClient);

        // when
        final URL resource =
            Thread.currentThread().getContextClassLoader().getResource("route-rule-reviews-test-v2.yaml");
        final List<IstioResource> istioResources = istioAssistant.deployIstioResources(resource);

        // then
        assertThat(istioResources)
            .hasSize(1);

    }

    @Test
    public void should_load_all_routes_from_classpath() {

        // given
        final IstioAssistant istioAssistant = new IstioAssistant((istioClient));

        // when
        final List<IstioResource> istioResources =
            istioAssistant.deployIstioResourcesFromClasspathPattern("route-rule-.*");

        // then
        assertThat(istioResources)
            .hasSize(2);
    }

    @Test
    public void should_load_all_routes_from_path() throws IOException {

        // given
        final IstioAssistant istioAssistant = new IstioAssistant(istioClient);

        // when
        final List<IstioResource> istioResources =
            istioAssistant.deployIstioResources(Paths.get(".","src", "test", "resources"));

        // then
        assertThat(istioResources)
            .hasSize(3);
    }


}
