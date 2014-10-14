package org.arquillian.cube.client;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class CubeConfigurationTest {

    private static final String CONTENT = "tomcat:\n" + 
            "  image: tutum/tomcat:7.0\n" + 
            "  exposedPorts: [8089/tcp]\n" + 
            "  await:\n" + 
            "    strategy: static\n" + 
            "    ip: localhost\n" + 
            "    ports: [8080, 8089]";
    
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
    
    @Test
    public void should_parse_and_load_configuration_file() {

        Map<String, String> parameters = new HashMap<String, String>();
        
        parameters.put("serverVersion", "1.13");
        parameters.put("serverUri", "http://localhost:25123");
        parameters.put("dockerContainers", CONTENT);
        
        CubeConfiguration cubeConfiguration = CubeConfiguration.fromMap(parameters);
        assertThat(cubeConfiguration.getDockerServerUri(), is("http://localhost:25123"));
        assertThat(cubeConfiguration.getDockerServerVersion(), is("1.13"));
        
        Map<String, Object> dockerContainersContent = cubeConfiguration.getDockerContainersContent();
        Map<String, Object> actualTomcat = (Map<String, Object>) dockerContainersContent.get("tomcat");
        assertThat(actualTomcat, is(notNullValue()));
        
        String image = (String) actualTomcat.get("image");
        assertThat(image, is("tutum/tomcat:7.0"));
        
    }
    
    @Test
    public void should_parse_and_load_configuration_file_from_container_configuration_file() throws IOException {

        File newFile = testFolder.newFile("config.yaml");
        Files.write(Paths.get(newFile.toURI()), CONTENT.getBytes());
        
        Map<String, String> parameters = new HashMap<String, String>();
        
        parameters.put("serverVersion", "1.13");
        parameters.put("serverUri", "http://localhost:25123");
        parameters.put("dockerContainersFile", newFile.getAbsolutePath());
        
        CubeConfiguration cubeConfiguration = CubeConfiguration.fromMap(parameters);
        assertThat(cubeConfiguration.getDockerServerUri(), is("http://localhost:25123"));
        assertThat(cubeConfiguration.getDockerServerVersion(), is("1.13"));
        
        Map<String, Object> dockerContainersContent = cubeConfiguration.getDockerContainersContent();
        Map<String, Object> actualTomcat = (Map<String, Object>) dockerContainersContent.get("tomcat");
        assertThat(actualTomcat, is(notNullValue()));
        
        String image = (String) actualTomcat.get("image");
        assertThat(image, is("tutum/tomcat:7.0"));
        
    }
    
}
