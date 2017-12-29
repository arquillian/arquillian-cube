package org.arquillian.cube.containerobject.dsl;

import com.github.dockerjava.api.DockerClient;
import java.util.List;
import org.arquillian.cube.docker.impl.client.containerobject.dsl.DockerNetwork;
import org.arquillian.cube.docker.impl.client.containerobject.dsl.Network;
import org.arquillian.cube.docker.impl.requirement.RequiresDockerMachine;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;


@Category(RequiresDockerMachine.class)
@RequiresDockerMachine(name = "dev")
@RunWith(ArquillianConditionalRunner.class)
public class NetworkTest {

    @DockerNetwork
    Network network = Network.withDefaultDriver("mynetwork").build();

    @ArquillianResource
    DockerClient dockerClient;

    @Test
    public void should_create_network() {

        final List<com.github.dockerjava.api.model.Network> mynetwork = dockerClient.listNetworksCmd()
            .withNameFilter("mynetwork").exec();
        assertThat(mynetwork)
            .hasSize(1)
            .extracting(com.github.dockerjava.api.model.Network::getName)
            .contains("mynetwork");
    }
}
