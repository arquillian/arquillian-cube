package org.arquillian.cube.kubernetes;

import io.fabric8.kubernetes.api.model.v4_0.ObjectMeta;
import io.fabric8.kubernetes.api.model.v4_0.Service;
import io.fabric8.kubernetes.api.model.v4_0.ServiceList;
import io.fabric8.kubernetes.clnt.v4_0.KubernetesClient;
import org.arquillian.cube.kubernetes.annotations.KubernetesResource;
import org.arquillian.cube.kubernetes.impl.requirement.RequiresKubernetes;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@Category(RequiresKubernetes.class)
@RequiresKubernetes
@RunWith(ArquillianConditionalRunner.class)
@KubernetesResource("classpath:hello-world.yaml")
public class HelloWorldResourcesIT {

    @ArquillianResource
    private KubernetesClient kubernetesClient;

    @Test
    public void shouldFindServiceInstance() throws IOException {
        final ServiceList service = kubernetesClient.services().list();

        assertThat(service.getItems())
            .hasSize(1)
            .extracting(Service::getMetadata)
            .extracting(ObjectMeta::getName)
            .containsExactlyInAnyOrder("hello-world");
    }

    @Test
    @KubernetesResource("classpath:hello-world-2.yaml")
    public void should_apply_route_programmatically() throws IOException {
        final ServiceList service = kubernetesClient.services().list();

        assertThat(service.getItems())
            .hasSize(2)
            .extracting(Service::getMetadata)
            .extracting(ObjectMeta::getName)
            .containsExactlyInAnyOrder("hello-world", "hello-world-2");
    }
}
