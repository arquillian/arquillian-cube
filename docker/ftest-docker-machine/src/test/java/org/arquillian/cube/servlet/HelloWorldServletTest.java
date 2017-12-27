package org.arquillian.cube.servlet;

import com.github.dockerjava.api.DockerClient;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import org.arquillian.cube.ChangeLog;
import org.arquillian.cube.CubeController;
import org.arquillian.cube.CubeID;
import org.arquillian.cube.HostIp;
import org.arquillian.cube.HostPort;
import org.arquillian.cube.TopContainer;
import org.arquillian.cube.docker.impl.requirement.RequiresDockerMachine;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.jboss.arquillian.container.test.api.Deployment;
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

@RunWith(ArquillianConditionalRunner.class)
@RequiresDockerMachine(name = "dev")
@Category(RequiresDockerMachine.class)
public class HelloWorldServletTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    @HostIp
    String hostIp;

    @HostPort(containerName = "tomcat", value = 8080)
    int tomcatPort;

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

        assertThat(response.toString(), is("{  \"status\": \"OK\"}"));
    }

    @Test
    public void should_get_running_logs(@ArquillianResource CubeController cubeController,
        @ArquillianResource CubeID cubeID) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        cubeController.copyLog(cubeID, false, true, true, false, -1, bos);
        String log = new String(bos.toByteArray());
        assertThat(log, notNullValue());
    }

    @Test
    public void should_execute_top(@ArquillianResource CubeController cubeController, @ArquillianResource CubeID cubeID) {
        TopContainer top = cubeController.top(cubeID);
        assertThat(top, notNullValue());
        assertThat(top.getProcesses(), notNullValue());
        assertThat(top.getTitles(), notNullValue());
    }

    @Test
    public void should_get_changes_on_container(@ArquillianResource CubeController cubeController,
        @ArquillianResource CubeID cubeID) {
        List<ChangeLog> changesOnFilesystem = cubeController.changesOnFilesystem(cubeID);
        assertThat(changesOnFilesystem, notNullValue());
        assertThat(changesOnFilesystem.size() > 0, is(true));
    }

    @Test
    public void should_copy_files_from_container(@ArquillianResource CubeController cubeController,
        @ArquillianResource CubeID cubeID) throws IOException {
        File newFolder = folder.newFolder();
        cubeController.copyFileDirectoryFromContainer(cubeID, "/tomcat/logs", newFolder.getAbsolutePath());
        File logFolder = newFolder.listFiles()[0];
        assertThat(logFolder, notNullValue());
        assertThat(logFolder.listFiles().length > 0, is(true));
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

    @Test
    public void should_enrich_test_with_ports() {
        assertThat(tomcatPort, is(8081));
    }

    @Test
    public void should_enrich_test_with_host_ip() {
        assertThat(hostIp, notNullValue());
    }

    @Test
    public void should_enrich_test_method_with_host_ip(@HostIp String methodHostIp) {
        assertThat(methodHostIp, notNullValue());
    }
}
