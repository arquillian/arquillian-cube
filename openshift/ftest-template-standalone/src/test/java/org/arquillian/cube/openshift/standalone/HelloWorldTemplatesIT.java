package org.arquillian.cube.openshift.standalone;

import java.io.IOException;
import java.net.URL;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.arquillian.cube.openshift.api.Template;
import org.arquillian.cube.openshift.api.TemplateParameter;
import org.arquillian.cube.openshift.api.Templates;
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
@Templates(templates = {@Template(url = "classpath:hello-openshift.yaml",
        parameters = @TemplateParameter(name = "RESPONSE", value = "Hello from Arquillian Templates")),
    @Template(url = "classpath:hello-openshift-templates.yaml",
        parameters = @TemplateParameter(name = "RESPONSE", value = "Hello from Arquillian Templates"))
})
public class HelloWorldTemplatesIT {

    @RouteURL("hello-openshift-route")
    @AwaitRoute
    private URL helloOpenshift;

    @RouteURL("hello-openshift-templates-route")
    @AwaitRoute
    private URL helloOpenshiftTemplates;

    @Test
    public void should_create_class_template_resources_from_first_template() throws IOException {
        verifyResponse(helloOpenshift);
    }

    @Test
    public void should_create_class_template_resources_from_second_template() throws IOException {
        verifyResponse(helloOpenshiftTemplates);
    }
    private void verifyResponse(URL url) throws IOException {
        assertThat(url).isNotNull();
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder().get().url(url).build();
        Response response = okHttpClient.newCall(request).execute();

        assertThat(response).isNotNull();
        assertThat(response.code()).isEqualTo(200);
        assertThat(response.body().string()).isEqualTo("Hello from Arquillian Templates\n");
    }
}

