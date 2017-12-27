package org.arquillian.cube;

import org.arquillian.cube.docker.impl.requirement.RequiresDockerMachine;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

@RunWith(ArquillianConditionalRunner.class)
@RequiresDockerMachine(name = "dev")
@Category(RequiresDockerMachine.class)
public class StandaloneTestCase {

    private static final String CONTAINER = "database";

    @ArquillianResource
    private CubeController cc;

    @Test @InSequence(0)
    public void shouldBeAbleToInjectController() {
        Assert.assertNotNull(cc);
    }

    @Test @InSequence(1)
    public void shouldBeAbleToCreateAndStart() {
        cc.create(CONTAINER);
        cc.start(CONTAINER);
    }

    @Test @InSequence(2)
    public void shouldBeAbleToStopAndDestroy() {
        cc.stop(CONTAINER);
        cc.destroy(CONTAINER);
    }
}
