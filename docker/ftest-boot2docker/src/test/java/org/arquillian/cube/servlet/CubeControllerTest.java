package org.arquillian.cube.servlet;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.arquillian.cube.CubeController;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.arquillian.cube.requirement.RequiresSystemPropertyOrEnvironmentVariable;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ArquillianConditionalRunner.class)
@RequiresSystemPropertyOrEnvironmentVariable("docker.tomcat.host")
public class CubeControllerTest {

    private static final String MANUAL_START_CUBE = "database_manual";

    @Deployment
    public static WebArchive create() {
        return ShrinkWrap.create(WebArchive.class).addClass(HelloWorldServlet.class);
    }

    @ArquillianResource
    private CubeController cubeController;


    /**
     * This test should run in the tomcat container.  This means the tomcat container is responsible
     * for starting our manual test. This is ordered and runs before the test below this, which runs in the JVM
     * running the maven command locally
     */
    @Test
    @InSequence(1)
    public void should_enrich_test_with_cube_controller_in_container() {
        assertThat(cubeController, notNullValue());
        cubeController.create(MANUAL_START_CUBE);
        cubeController.start(MANUAL_START_CUBE);
    }


    /**
     * Ensure that as a different environment we can stop the cube controller.
     */
    @Test
    @InSequence(2)
    @RunAsClient
    public void should_enrich_test_with_cube_controller() {
        assertThat(cubeController, notNullValue());
        cubeController.stop(MANUAL_START_CUBE);
        cubeController.destroy(MANUAL_START_CUBE);
    }
}
