package org.arquillian.cube.openshift.standalone;

import io.fabric8.kubernetes.api.model.Pod;
import org.arquillian.cube.kubernetes.annotations.Named;
import org.arquillian.cube.kubernetes.annotations.PortForward;
import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import static org.junit.Assert.assertNotNull;

@RunWith(ArquillianConditionalRunner.class)
@RequiresOpenshift
public class HelloWorldTest {

    @Named("hello-openshift")
    @PortForward
    @ArquillianResource
    Pod pod;

    @Test
    public void shouldShowHelloWorld() throws IOException {
        assertNotNull(pod);
    }
}
