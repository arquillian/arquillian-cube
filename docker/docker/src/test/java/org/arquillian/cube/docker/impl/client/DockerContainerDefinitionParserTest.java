package org.arquillian.cube.docker.impl.client;

import java.net.URI;
import org.arquillian.cube.docker.impl.requirement.RequiresDocker;
import org.arquillian.cube.docker.impl.requirement.RequiresDockerMachine;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.assertThat;

@Category({RequiresDocker.class, RequiresDockerMachine.class})
public class DockerContainerDefinitionParserTest {

    @Test
    public void should_get_uri_from_src_test_or_main_resources() {
        final URI resources =
            DockerContainerDefinitionParser.checkSrcTestAndMainResources("simple-docker-compose", "resources");
        assertThat(resources).isNotNull();
    }

    @Test
    public void should_get_null_uri_from_src_test_or_main_resources() {
        final URI resources =
            DockerContainerDefinitionParser.checkSrcTestAndMainResources("none-existing-simple-docker-compose",
                "resources");
        assertThat(resources).isNull();
    }

    @Test
    public void should_get_uri_from_src_test_or_main_resources_subdirectory() {
        final URI resources =
            DockerContainerDefinitionParser.checkSrcTestAndMainResources("simple-docker-compose-v2", "resources/myapp");
        assertThat(resources).isNotNull();
    }
}
