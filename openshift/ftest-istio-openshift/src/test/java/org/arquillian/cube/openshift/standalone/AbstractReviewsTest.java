package org.arquillian.cube.openshift.standalone;

import io.fabric8.kubernetes.client.http.HttpClient;
import io.fabric8.kubernetes.client.http.HttpRequest;
import io.fabric8.kubernetes.client.http.HttpResponse;
import io.fabric8.kubernetes.client.jdkhttp.JdkHttpClientFactory;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.path.json.JsonPath;
import io.restassured.specification.RequestSpecification;
import org.arquillian.cube.istio.impl.IstioAssistant;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasKey;

public abstract class AbstractReviewsTest {
    private HttpClient.Builder httpClientBuilder = new JdkHttpClientFactory().newBuilder();

    protected void should_get_v1_if_not_logged(URL url) {
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

    protected void alex_should_use_reviews_v2_version(URL url,
        IstioAssistant istioAssistant) throws IOException, ExecutionException, InterruptedException {
        // given
        waitUntilRouteIsPopulated(url, istioAssistant);

        // when

        // This was previously using okhttp because we didn't find any way of making rest assured working when setting
        // the required cookies. Now we switch to JdkHttpClient, to see whether we can avoid depending on
        // okhttp artifacts at all
        final HttpClient httpClient = httpClientBuilder.build();
        final HttpRequest request = httpClient.newHttpRequestBuilder()
            .url(new URL(url.toString() + "api/v1/products/0/reviews"))
            .header("Cookie", "user=alex; Domain=" + url.getHost() +"; Path=/")
            .build();

        HttpResponse<String> response = httpClient.sendAsync(request, String.class).get();

            // then

            final String content = response.body();

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

    protected void waitUntilRouteIsPopulated(URL url, IstioAssistant istioAssistant) throws MalformedURLException {
        final HttpClient httpClient = httpClientBuilder.build();
        final HttpRequest request = httpClient.newHttpRequestBuilder()
            .url(new URL(url.toString() + "api/v1/products/0/reviews"))
            .header("Cookie", "user=alex; Domain=" + url.getHost() +"; Path=/")
            .build();

        istioAssistant.await(request, response -> {
            return response.body().contains("stars");
        });
    }
}
