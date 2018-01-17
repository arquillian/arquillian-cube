package org.arquillian.cube.openshift.standalone;

import java.io.IOException;
import java.net.URL;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.arquillian.cube.openshift.api.Template;
import org.arquillian.cube.openshift.api.TemplateParameter;
import org.arquillian.cube.openshift.impl.enricher.AwaitRoute;
import org.arquillian.cube.openshift.impl.enricher.RouteURL;
import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(ArquillianConditionalRunner.class)
@RequiresOpenshift
@Template(url = "https://gist.githubusercontent.com/lordofthejars/8781cacd4000a516695ad6c55b5815b3/raw/5151aeef0f5dd8823e2c581c3b7452f04a76af59/hello-template.yaml",
          parameters = @TemplateParameter(name = "RESPONSE", value = "Hello from Arquillian Template"))
public class HelloWorldTemplateTest {

    @RouteURL("hello-openshift-route")
    @AwaitRoute
    private URL url;

    @Test
    public void should_create_class_template_resources() throws IOException {
        verifyResponse(url);
    }

    @Test
    @Template(url = "https://gist.githubusercontent.com/dipak-pawar/403b870fc92f6569f64f12b506318606/raw/4dd7cd4b259f893353509411ba4777792cacd034/hello_openshift_route_template.yaml",
        parameters = @TemplateParameter(name = "ROUTE_NAME", value = "hello-openshift-method-route"))
    public void should_create_method_template_resources(
        @RouteURL("hello-openshift-method-route") @AwaitRoute URL routeUrl)
        throws IOException {
        verifyResponse(routeUrl);
    }

    private void verifyResponse(URL url) throws IOException {
        assertThat(url).isNotNull();
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder().get().url(url).build();
        Response response = okHttpClient.newCall(request).execute();

        assertThat(response).isNotNull();
        assertThat(response.code()).isEqualTo(200);
        assertThat(response.body().string()).isEqualTo("Hello from Arquillian Template\n");
    }
}
