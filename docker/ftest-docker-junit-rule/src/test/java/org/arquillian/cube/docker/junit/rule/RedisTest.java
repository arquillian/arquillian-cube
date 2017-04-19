package org.arquillian.cube.docker.junit.rule;

import org.junit.ClassRule;
import org.junit.Test;
import redis.clients.jedis.Jedis;

import static org.assertj.core.api.Assertions.assertThat;

public class RedisTest {

    @ClassRule
    public static ContainerDslRule redis = new ContainerDslRule("redis:3.2.6")
                                                                .withPortBinding(6379);

    @Test
    public void should_insert_string_in_redis() {
        Jedis jedis = new Jedis(redis.getIpAddress(), redis.getBindPort(6379));
        jedis.set("foo", "bar");

        assertThat(jedis.get("foo")).isEqualTo("bar");
    }

}
