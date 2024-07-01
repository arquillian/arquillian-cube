package org.arquillian.cube.docker.junit.rule;

import org.arquillian.cube.docker.impl.requirement.RequiresDocker;

import org.arquillian.cube.requirement.RequirementRule;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.assertThat;

@Category({ RequiresDocker.class})

public class NetworkIT {

    @Rule
    public RequirementRule requirementRule = new RequirementRule();

    @ClassRule
    public static final NetworkDslRule network = new NetworkDslRule("mynetwork");

    @Test
    public void should_create_networks() {
        assertThat(network.getNetworks()).contains(network.getNetworkName());
    }

}
