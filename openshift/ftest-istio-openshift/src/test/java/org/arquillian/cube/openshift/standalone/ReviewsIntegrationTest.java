package org.arquillian.cube.openshift.standalone;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.path.json.JsonPath;
import io.restassured.specification.RequestSpecification;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.arquillian.cube.istio.api.IstioResource;
import org.arquillian.cube.istio.impl.IstioAssistant;
import org.arquillian.cube.openshift.impl.enricher.AwaitRoute;
import org.arquillian.cube.openshift.impl.enricher.RouteURL;
import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.assertj.core.api.Assertions;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasKey;

@RunWith(ArquillianConditionalRunner.class)
@Category(RequiresOpenshift.class)
@RequiresOpenshift
@IstioResource("route-rule-reviews-test-v${serviceVersion:2}.yaml")
@Ignore("This test assumes that you have a cluster installed with Istio and BookInfo application deployed. We could make a full test preparing all this, but it will take lot of time, not error safe and test execution would take like 10 minutes")
public class ReviewsIntegrationTest {

    @RouteURL("productpage")
    @AwaitRoute
    private URL url;

    @ArquillianResource
    private IstioAssistant istioAssistant;

    @Test
    public void should_get_v1_if_not_logged() {

        // given
        final RequestSpecification requestConfiguration = new RequestSpecBuilder()
            .setBaseUri(url.toString())
            .build();

        // when
        given()
            .spec(requestConfiguration)
            .when()
            .get("api/v1/products/{productId}/reviews", 0)
            .then()
            .assertThat()
            .statusCode(200)
            .body("$.reviews", not(hasKey("rating")));

    }

    @Test
    public void alex_should_use_reviews_v2_version() throws IOException {

        // given
        waitUntilRouteIsPopulated();

        // when

        // Using okhttp because I have not find any way of making rest assured working when setting the required cookies
        final Request request = new Request.Builder()
            .url(url.toString() + "api/v1/products/0/reviews")
            .addHeader("Cookie", "user=alex; Domain=" + url.getHost() +"; Path=/")
            .build();

        final OkHttpClient okHttpClient = new OkHttpClient();
        try(Response response = okHttpClient.newCall(request).execute()) {

            // then

            final String content = response.body().string();

            final List<Map<String, Object>> ratings = JsonPath.from(content).getList("reviews.rating");

            final Map<String, Object> expectationStar5 = new HashMap<>();
            expectationStar5.put("color", "black");
            expectationStar5.put("stars", 5);

            final Map<String, Object> expectationStar4 = new HashMap<>();
            expectationStar4.put("color", "black");
            expectationStar4.put("stars", 4);

            assertThat(ratings)
                .containsExactlyInAnyOrder(expectationStar4, expectationStar5);

        }

    }

    private void waitUntilRouteIsPopulated() {
        final Request request = new Request.Builder()
            .url(url.toString() + "api/v1/products/0/reviews")
            .addHeader("Cookie", "user=alex; Domain=" + url.getHost() +"; Path=/")
            .build();

        istioAssistant.await(request, response -> {
            try {
                return response.body().string().contains("stars");
            } catch (IOException e) {
                return false;
            }
        });
    }

    private void waitUntilRouteIsPopulated2() {
        final Request request = new Request.Builder()
            .url(url.toString() + "api/v1/products/0/reviews")
            .addHeader("Cookie", "user=alex; Domain=" + url.getHost() +"; Path=/")
            .build();

        istioAssistant.await(request, response -> "2.0.0".equals(response.header("version")));
    }

}
