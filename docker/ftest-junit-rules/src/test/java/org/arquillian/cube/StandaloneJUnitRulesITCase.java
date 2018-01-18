package org.arquillian.cube;

import org.arquillian.cube.docker.impl.requirement.RequiresDocker;
import org.arquillian.cube.docker.impl.requirement.RequiresDockerMachine;
import org.jboss.arquillian.junit.ArquillianTest;
import org.jboss.arquillian.junit.ArquillianTestClass;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category({RequiresDocker.class, RequiresDockerMachine.class})
@RequiresDocker
public class StandaloneJUnitRulesITCase {

    @ClassRule
    public static ArquillianTestClass arquillianTestClass = new ArquillianTestClass();

    @Rule
    public ArquillianTest arquillianTest = new ArquillianTest();

    private static final String CONTAINER = "database";

    @ArquillianResource
    private CubeController cc;

    @Test
    @InSequence(0)
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
