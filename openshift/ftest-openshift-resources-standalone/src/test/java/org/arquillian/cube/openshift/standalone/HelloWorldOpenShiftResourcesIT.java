package org.arquillian.cube.openshift.standalone;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteList;
import io.fabric8.openshift.client.OpenShiftClient;
import java.io.IOException;
import java.net.URL;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.arquillian.cube.openshift.api.OpenShiftResource;
import org.arquillian.cube.openshift.impl.enricher.AwaitRoute;
import org.arquillian.cube.openshift.impl.enricher.RouteURL;
import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;

@Category(RequiresOpenshift.class)
@RequiresOpenshift
@RunWith(ArquillianConditionalRunner.class)
@OpenShiftResource("classpath:hello-route.yaml")
public class HelloWorldOpenShiftResourcesIT {

    // tag::enricher_expression_resolver_example[]
    @RouteURL("${route.name}")
    @AwaitRoute
    private URL url;
    // end::enricher_expression_resolver_example[]

    @ArquillianResource
    private OpenShiftClient openShiftClient;

    @Test
    public void should_show_hello_world() throws IOException {
        assertThat(url).isNotNull();
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder().get().url(url).build();
        Response response = okHttpClient.newCall(request).execute();

        assertThat(response).isNotNull();
        assertThat(response.code()).isEqualTo(200);
        assertThat(response.body().string()).isEqualTo("Hello OpenShift!\n");
    }

    @Test
    @OpenShiftResource("classpath:hello-route-2.yaml")
    public void should_register_extra_route() {
        final RouteList routes = openShiftClient.routes().list();
        assertThat(routes.getItems())
            .hasSize(2)
            .extracting(Route::getMetadata)
            .extracting(ObjectMeta::getName)
            .containsExactlyInAnyOrder("hello-world", "hello-world-2");
    }

}
