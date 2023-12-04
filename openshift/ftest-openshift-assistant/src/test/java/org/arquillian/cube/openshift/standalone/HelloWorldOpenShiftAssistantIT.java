package org.arquillian.cube.openshift.standalone;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.arquillian.cube.openshift.impl.client.OpenShiftAssistant;
import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.internal.readiness.Readiness;

import static org.assertj.core.api.Assertions.assertThat;

@Category(RequiresOpenshift.class)
@RequiresOpenshift
@RunWith(ArquillianConditionalRunner.class)
public class HelloWorldOpenShiftAssistantIT {

    @ArquillianResource
    private OpenShiftAssistant openShiftAssistant;

    @Test
    public void should_inject_openshift_assistant() {
        assertThat(openShiftAssistant).isNotNull();
    }

    @Test
    public void should_deploy_app_programmatically() throws IOException {
        openShiftAssistant.deployApplication("hello-openshift-deployment-config", "deployment.yml");

        openShiftAssistant.awaitApplicationReadinessOrFail();

        List<Pod> pods = openShiftAssistant.getClient()
            .pods()
            .inNamespace(openShiftAssistant.getCurrentProjectName())
            .withLabel("name", "hello-openshift-deployment-config")
            .list()
            .getItems();
        assertThat(pods.size()).isGreaterThan(1);
        assertThat(pods).allMatch(Readiness::isPodReady);
    }

    @Test
    public void should_apply_route_programmatically() throws IOException {

        openShiftAssistant.deployApplication("hello-world", "hello-route.json");

        final Optional<URL> route = openShiftAssistant.getRoute();
        openShiftAssistant.awaitUrl(route.get());


        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder().get().url(route.get()).build();
        Response response = okHttpClient.newCall(request).execute();

        assertThat(response).isNotNull();
        assertThat(response.code()).isEqualTo(200);
        assertThat(response.body().string()).isEqualTo("Hello OpenShift!\n");
    }


}
