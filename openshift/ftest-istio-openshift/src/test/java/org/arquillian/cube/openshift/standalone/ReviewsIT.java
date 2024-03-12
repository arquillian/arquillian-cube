package org.arquillian.cube.openshift.standalone;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ExecutionException;

import org.arquillian.cube.istio.api.IstioResource;
import org.arquillian.cube.istio.impl.IstioAssistant;
import org.arquillian.cube.openshift.impl.enricher.AwaitRoute;
import org.arquillian.cube.openshift.impl.enricher.RouteURL;
import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

@RunWith(ArquillianConditionalRunner.class)
@Category(RequiresOpenshift.class)
@RequiresOpenshift
@IstioResource("classpath:route-rule-reviews-test-v${serviceVersion:2}.yaml")
@Ignore("This test assumes that you have a cluster installed with Istio and BookInfo application deployed. We could make a full test preparing all this, but it will take lot of time, not error safe and test execution would take like 10 minutes")
public class ReviewsIT extends AbstractReviewsTest {

    @RouteURL("productpage")
    @AwaitRoute
    private URL url;

    // tag::istio_assistant[]
    @ArquillianResource
    private IstioAssistant istioAssistant;
    // end::istio_assistant[]

    @Test
    public void should_get_v1_if_not_logged() {
        should_get_v1_if_not_logged(url);
    }

    @Test
    public void alex_should_use_reviews_v2_version() throws IOException, ExecutionException, InterruptedException {
        alex_should_use_reviews_v2_version(url, istioAssistant);
    }

}
