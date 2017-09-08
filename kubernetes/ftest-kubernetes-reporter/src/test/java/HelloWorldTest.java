import io.fabric8.kubernetes.api.model.v2_6.Service;
import java.io.IOException;
import org.arquillian.cube.kubernetes.annotations.Named;
import org.arquillian.cube.kubernetes.impl.requirement.RequiresKubernetes;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

@RunWith(ArquillianConditionalRunner.class)
@RequiresKubernetes
public class HelloWorldTest {

    @Named("hello-world-service")
    @ArquillianResource
    Service helloWorld;

    @Test
    public void shouldShowHelloWorld() throws IOException {
        assertNotNull(helloWorld);
        assertNotNull(helloWorld.getSpec());
        assertNotNull(helloWorld.getSpec().getPorts());
        assertFalse(helloWorld.getSpec().getPorts().isEmpty());
    }
}
