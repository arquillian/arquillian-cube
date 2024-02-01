import io.fabric8.kubernetes.api.model.Service;
import java.io.IOException;
import java.net.URL;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import org.arquillian.cube.kubernetes.annotations.Named;
import org.arquillian.cube.kubernetes.annotations.PortForward;
import org.arquillian.cube.kubernetes.impl.requirement.RequiresKubernetes;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import static junit.framework.TestCase.assertFalse;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@Category(RequiresKubernetes.class)
@RequiresKubernetes
@RunWith(ArquillianConditionalRunner.class)
public class HelloWorldIT {

    @Named("hello-world")
    @ArquillianResource
    private Service helloWorld;

    @Named("hello-world")
    @PortForward
    @ArquillianResource
    private URL url;

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
        //#641 mentions issues on repeated calls to the portforwarded service.
        for (int i = 0; i < 5; i++) {
            RestAssured.given()
                .when()
                .get(url)
                .then()
                .assertThat()
                .statusCode(200)
                .body(contains("Hello OpenShift!\n"));
        }
    }
}
