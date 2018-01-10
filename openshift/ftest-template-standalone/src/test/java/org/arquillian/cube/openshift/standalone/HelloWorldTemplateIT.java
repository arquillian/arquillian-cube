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
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;

@Category(RequiresOpenshift.class)
@RequiresOpenshift
@RunWith(ArquillianConditionalRunner.class)
@Template(url = "https://gist.githubusercontent.com/lordofthejars/8781cacd4000a516695ad6c55b5815b3/raw/5151aeef0f5dd8823e2c581c3b7452f04a76af59/hello-template.yaml",
          parameters = @TemplateParameter(name = "RESPONSE", value = "Hello from Arquillian Template"))
public class HelloWorldTemplateIT {

    @RouteURL("hello-openshift-route")
    @AwaitRoute
    private URL url;

    @Test
    public void should_show_hello_world() throws IOException {
        assertThat(url).isNotNull();
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder().get().url(url).build();
        Response response = okHttpClient.newCall(request).execute();

        assertThat(response).isNotNull();
        assertThat(response.code()).isEqualTo(200);
        assertThat(response.body().string()).isEqualTo("Hello from Arquillian Template\n");
    }

}
