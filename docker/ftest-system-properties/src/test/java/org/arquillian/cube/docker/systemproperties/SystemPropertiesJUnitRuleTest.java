package org.arquillian.cube.docker.systemproperties;

import org.arquillian.cube.docker.impl.requirement.RequiresDockerMachine;
import org.arquillian.cube.docker.junit.rule.ContainerDslRule;
import org.arquillian.cube.requirement.RequirementRule;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import redis.clients.jedis.Jedis;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@RequiresDockerMachine(name = "dev")
public class SystemPropertiesJUnitRuleTest {

    @Rule
    public RequirementRule requirementRule = new RequirementRule();

    @ClassRule
    public static ContainerDslRule redis = new ContainerDslRule("redis:3.2.6")
                                                                .withPortBinding(6379);

    @Test
    public void should_add_system_properties() {

        // when

        final Properties properties = System.getProperties();

        // then

        assertThat(properties)
            .containsKey("arq.cube.docker.host")
            .containsKey("arq.cube.docker.redis_3_2_6.ip")
            .containsKey("arq.cube.docker.redis_3_2_6.internal.ip")
            .containsEntry("arq.cube.docker.redis_3_2_6.port.6379", "6379");
    }

}
