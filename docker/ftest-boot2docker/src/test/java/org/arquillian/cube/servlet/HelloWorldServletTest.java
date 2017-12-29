package org.arquillian.cube.servlet;

import com.github.dockerjava.api.DockerClient;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.arquillian.cube.CubeController;
import org.arquillian.cube.CubeID;
import org.arquillian.cube.docker.impl.requirement.RequiresDocker;
import org.arquillian.cube.docker.impl.requirement.RequiresDockerMachine;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.arquillian.cube.requirement.RequiresSystemPropertyOrEnvironmentVariable;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

@Category({RequiresDocker.class, RequiresDockerMachine.class})
@RunWith(ArquillianConditionalRunner.class)
@RequiresSystemPropertyOrEnvironmentVariable("docker.tomcat.host")
public class HelloWorldServletTest {

    @Deployment(testable = false)
    public static WebArchive create() {
        return ShrinkWrap.create(WebArchive.class, "hello.war").addClass(HelloWorldServlet.class);
    }

    @Test
    public void should_parse_and_load_configuration_file(@ArquillianResource URL base) throws IOException {

        URL obj = new URL(base, "HelloWorld");
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(
            new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        assertThat(response.toString(), is("Hello World"));
    }

    @Test
    public void should_enrich_test_with_docker_client(@ArquillianResource CubeController cubeController) {
        assertThat(cubeController, notNullValue());
    }

    @Test
    public void should_enrich_test_with_docker_client(@ArquillianResource DockerClient dockerClient) {
        assertThat(dockerClient, notNullValue());
    }

    @Test
    public void should_enrich_test_with_cube_id(@ArquillianResource CubeID cubeId) {
        assertThat(cubeId, notNullValue());
    }
}
