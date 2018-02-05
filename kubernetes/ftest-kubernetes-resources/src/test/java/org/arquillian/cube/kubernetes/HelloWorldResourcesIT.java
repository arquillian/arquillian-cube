package org.arquillian.cube.kubernetes;

import io.fabric8.kubernetes.api.model.v3_1.Service;
import org.arquillian.cube.kubernetes.annotations.KubernetesResource;
import org.arquillian.cube.kubernetes.annotations.Named;
import org.arquillian.cube.kubernetes.impl.requirement.RequiresKubernetes;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.io.IOException;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertNotNull;

@Category(RequiresKubernetes.class)
@RequiresKubernetes
@RunWith(ArquillianConditionalRunner.class)
@KubernetesResource("classpath:hello-world.yaml")
public class HelloWorldResourcesIT {

    @Named("hello-world")
    @ArquillianResource
    private Service helloWorld;

    @Test
    public void shouldFindServiceInstance() throws IOException {
        assertNotNull(helloWorld);
        assertNotNull(helloWorld.getSpec());
        assertNotNull(helloWorld.getSpec().getPorts());
        assertFalse(helloWorld.getSpec().getPorts().isEmpty());
    }
}
