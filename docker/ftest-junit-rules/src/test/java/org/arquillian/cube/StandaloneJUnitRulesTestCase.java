package org.arquillian.cube;

import org.jboss.arquillian.junit.ArquillianTest;
import org.jboss.arquillian.junit.ArquillianTestClass;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

public class StandaloneJUnitRulesTestCase {

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
