package org.arquillian.cube.openshift.standalone;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.arquillian.cube.openshift.api.OpenShiftHandle;
import org.arquillian.cube.openshift.api.Template;
import org.arquillian.cube.openshift.impl.enricher.RouteURL;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Java6Assertions.assertThat;

/*
How to run this test:
mvn clean test -Dkubernetes.master=https://openshiftm:8443 -Drouter.ip=<OPENSHIFT_ADDRESS>
where OPENSHIFT_ADDRESS is the same IP than OpenShift master node.
 */
@RunWith(Arquillian.class)
@Template(url = "https://raw.githubusercontent.com/openshift/nodejs-ex/master/openshift/templates/nodejs.json")
public class SimpleTest {

    @RouteURL("nodejs-example")
    String url;

    @ArquillianResource
    OpenShiftHandle adapter;

    /* Arquillian Cube should wait for the pod to get ready before start the test */
    @Test
    public void testSomething() throws Exception {
        assertThat(adapter.getReadyPods("nodejs-ex").size()).isEqualTo(1);
        assertThat(url).isNotNull();
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder().get().url(url).build();
        Response response = okHttpClient.newCall(request).execute();
        assertThat(response).isNotNull();
        assertThat(response.code()).isEqualTo(200);
        assertThat(response.body().string()).contains("Welcome to your Node.js application on OpenShift");
    }
}
