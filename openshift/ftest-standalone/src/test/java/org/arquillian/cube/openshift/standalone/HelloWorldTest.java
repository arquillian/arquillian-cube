package org.arquillian.cube.openshift.standalone;

import io.fabric8.kubernetes.api.model.Service;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.arquillian.cube.kubernetes.annotations.Named;
import org.arquillian.cube.kubernetes.annotations.PortForward;
import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.URL;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;


@RunWith(ArquillianConditionalRunner.class)
@RequiresOpenshift
public class HelloWorldTest {

    @Named("hello-openshift")
    @PortForward
    @ArquillianResource
    Service service;

    @Named("hello-openshift")
    @PortForward
    @ArquillianResource
    URL url;

    @Test
    public void pod_instance_should_not_be_null() throws IOException {
        assertNotNull(service);
        assertNotNull(service.getSpec());
        assertNotNull(service.getSpec().getPorts());
        assertFalse(service.getSpec().getPorts().isEmpty());
    }

    @Test
    public void shouldShowHelloWorld() throws IOException {
        assertNotNull(url);
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder().get().url(url).build();
        Response response = okHttpClient.newCall(request).execute();
        assertNotNull(response);
        assertEquals(200, response.code());
        assertTrue(response.body().string().contains("Hello OpenShift!"));
    }
}
