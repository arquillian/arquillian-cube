package org.arquillian.cube.docker.junit5;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// tag::docs[]
//@ExtendWith(NetworkDslResolver.class)
// end::docs[]
@Disabled("Test disabled because requirements module has no support for JUnit5 yet")
// tag::docs[]
public class NetworkTest {
// end::docs[]

    // tag::docs[]
    private NetworkDsl networkDsl = new NetworkDsl("mynetwork");
    // end::docs[]

    @Test
    public void should_create_network() {

        assertThat(networkDsl.getNetworkName()).isEqualTo("mynetwork");

    }

// tag::docs[]
}
// end::docs[]
