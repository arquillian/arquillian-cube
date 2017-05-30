package org.arquillian.cube.docker.systemproperties;

import org.arquillian.cube.docker.impl.requirement.RequiresDockerMachine;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(ArquillianConditionalRunner.class)
@RequiresDockerMachine(name = "dev")
public class SystemPropertiesRunnerTest {

    @Test
    public void should_add_system_properties() {

        // when

        final Properties properties = System.getProperties();

        // then

        assertThat(properties)
            .containsKey("arq.cube.docker.host")
            .containsKey("arq.cube.docker.helloworld.ip")
            .containsKey("arq.cube.docker.helloworld.internal.ip")
            .containsEntry("arq.cube.docker.helloworld.port.8080", "8080");
    }


}
