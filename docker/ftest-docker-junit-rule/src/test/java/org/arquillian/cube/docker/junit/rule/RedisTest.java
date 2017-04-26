package org.arquillian.cube.docker.junit.rule;

import org.arquillian.cube.docker.impl.requirement.RequiresDockerMachine;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.arquillian.cube.requirement.RequirementRule;
import org.arquillian.cube.spi.CubeOutput;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import redis.clients.jedis.Jedis;

import static org.assertj.core.api.Assertions.assertThat;

@RequiresDockerMachine(name = "dev")
public class RedisTest {

    @Rule
    public RequirementRule requirementRule = new RequirementRule();

    @ClassRule
    public static ContainerDslRule redis = new ContainerDslRule("redis:3.2.6")
                                                                .withPortBinding(6379);

    @Test
    public void should_insert_string_in_redis() {
        Jedis jedis = new Jedis(redis.getIpAddress(), redis.getBindPort(6379));
        jedis.set("foo", "bar");

        assertThat(jedis.get("foo")).isEqualTo("bar");
    }

    @Test
    public void should_get_logs() {
        assertThat(redis.getLog())
            .isNotBlank()
            .contains("Redis");
    }

    @Test
    public void should_execute_uname() {
        assertThat(redis.exec("sh", "-c", "uname").getStandard())
            .isNotBlank()
            .isEqualToIgnoringWhitespace("Linux");

    }

}
