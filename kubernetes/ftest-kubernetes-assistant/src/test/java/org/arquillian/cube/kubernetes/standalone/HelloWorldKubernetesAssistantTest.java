package org.arquillian.cube.kubernetes.standalone;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.arquillian.cube.kubernetes.impl.KubernetesAssistant;
import org.arquillian.cube.kubernetes.impl.requirement.RequiresKubernetes;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(ArquillianConditionalRunner.class)
@RequiresKubernetes
public class HelloWorldKubernetesAssistantTest {

    @ArquillianResource
    private KubernetesAssistant kubernetesAssistant;

    @Test
    public void should_inject_kubernetes_assistant() {
        assertThat(kubernetesAssistant).isNotNull();
    }

    @Test
    public void should_apply_route_programmatically() throws IOException {
        kubernetesAssistant.deployApplication("hello-world");
        Optional<URL> serviceUrl = kubernetesAssistant.getServiceUrl("hello-world");

        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder().get().url(serviceUrl.get()).build();
        Response response = okHttpClient.newCall(request).execute();

        assertThat(response).isNotNull();
        assertThat(response.code()).isEqualTo(200);
        assertThat(response.body().string()).isEqualTo("Hello OpenShift!\n");
    }
}
