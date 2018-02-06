package org.arquillian.cube.openshift.standalone;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.arquillian.cube.openshift.impl.client.OpenShiftAssistant;
import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(ArquillianConditionalRunner.class)
@RequiresOpenshift
public class HelloWorldOpenShiftAssistantTest {

    @ArquillianResource
    private OpenShiftAssistant openShiftAssistant;

    @Test
    public void should_inject_openshift_assistant() {
        assertThat(openShiftAssistant).isNotNull();
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
