package org.arquillian.cube.openshift.standalone;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasKey;

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
import org.arquillian.cube.istio.impl.IstioAssistant;

public abstract class AbstractReviewsTest {

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
        IstioAssistant istioAssistant) throws IOException {
        // given
        waitUntilRouteIsPopulated(url, istioAssistant);

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

    protected void waitUntilRouteIsPopulated(URL url, IstioAssistant istioAssistant) {
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
}
