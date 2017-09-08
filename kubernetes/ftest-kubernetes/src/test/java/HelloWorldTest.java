import io.fabric8.kubernetes.api.model.v2_6.Service;
import java.io.IOException;
import java.net.URL;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.arquillian.cube.kubernetes.annotations.Named;
import org.arquillian.cube.kubernetes.annotations.PortForward;
import org.arquillian.cube.kubernetes.impl.requirement.RequiresKubernetes;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(ArquillianConditionalRunner.class)
@RequiresKubernetes
public class HelloWorldTest {

    @Named("hello-world")
    @ArquillianResource
    Service helloWorld;

    @Named("hello-world")
    @PortForward
    @ArquillianResource
    URL url;

    @Test
    public void shouldFindServiceInstance() throws IOException {
        assertNotNull(helloWorld);
        assertNotNull(helloWorld.getSpec());
        assertNotNull(helloWorld.getSpec().getPorts());
        assertFalse(helloWorld.getSpec().getPorts().isEmpty());
    }

    @Test
    public void shouldShowHelloWorld() throws IOException {
        assertNotNull(url);
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder().get().url(url).build();

        //#641 mentions issues on repeated calls to the portforwarded service.
        for (int i = 0; i < 5; i++) {
            Response response = okHttpClient.newCall(request).execute();
            assertNotNull(response);
            assertEquals(200, response.code());
            assertTrue(response.body().string().contains("Hello world!"));
        }
    }
}
