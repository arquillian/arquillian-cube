package org.arquillian.cube.docker.junit5;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import redis.clients.jedis.Jedis;

import static org.assertj.core.api.Assertions.assertThat;

// tag::docs[]
@ExtendWith(ContainerDslResolver.class)
// end::docs[]
@Disabled("Test disabled because requirements module has no support for JUnit5 yet")
// tag::docs[]
public class RedisIT {

    private ContainerDsl redis = new ContainerDsl("redis:3.2.6")
                                        .withPortBinding(6379);
// end::docs[]
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
// tag::docs[]
}
// end::docs[]
