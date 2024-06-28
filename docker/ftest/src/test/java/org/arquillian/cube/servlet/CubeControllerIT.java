package org.arquillian.cube.servlet;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;
import org.arquillian.cube.ChangeLog;
import org.arquillian.cube.CubeController;
import org.arquillian.cube.CubeID;
import org.arquillian.cube.TopContainer;
import org.arquillian.cube.docker.impl.requirement.RequiresDocker;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.arquillian.cube.requirement.RequiresSystemPropertyOrEnvironmentVariable;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

@Category({RequiresDocker.class})
@RunWith(ArquillianConditionalRunner.class)
@RequiresSystemPropertyOrEnvironmentVariable("docker.tomcat.host")
public class CubeControllerIT {

    private static final String MANUAL_START_CUBE = "database_manual";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder(new File("/tmp"));
    @ArquillianResource
    private CubeController cubeController;

    @Deployment
    public static WebArchive create() {
        return ShrinkWrap.create(WebArchive.class).addClass(HelloWorldServlet.class);
    }

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

    @Test(expected = UnsupportedOperationException.class)
    public void should_get_an_exception_when_getting_logs(@ArquillianResource CubeController cubeController,
        @ArquillianResource CubeID cubeID) {
        cubeController.copyLog(cubeID, false, true, true, false, -1, new ByteArrayOutputStream());
    }

    @Test
    public void should_execute_top(@ArquillianResource CubeController cubeController, @ArquillianResource CubeID cubeID) {
        TopContainer top = cubeController.top(cubeID);
        assertThat(top, notNullValue());
        assertThat(top.getProcesses(), notNullValue());
        assertThat(top.getTitles(), notNullValue());
        assertThat(top.getTitles().length > 0, is(true));
    }

    @Test
    public void should_get_changes_on_container(@ArquillianResource CubeController cubeController,
        @ArquillianResource CubeID cubeID) {
        List<ChangeLog> changesOnFilesystem = cubeController.changesOnFilesystem(cubeID);
        assertThat(changesOnFilesystem, notNullValue());
        assertThat(changesOnFilesystem.size() > 0, is(true));
    }
}
