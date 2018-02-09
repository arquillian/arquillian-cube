package org.arquillian.cube.docker.junit5;

import org.arquillian.cube.docker.impl.client.config.Link;
import org.arquillian.cube.docker.impl.client.containerobject.dsl.Container;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ContainerDslTest {

    @Test
    public void should_create_image_id_by_image_name() {

        // given
        ContainerDsl containerDsl = new ContainerDsl("fedora");

        // when
        final Container container = containerDsl.buildContainer();

        // then
        assertThat(container.getContainerName()).isEqualTo("fedora");
    }

    @Test
    public void should_create_image_id_from_image_name_with_version_chars() {

        // given
        ContainerDsl containerDsl = new ContainerDsl("fedora:10");

        // when
        final Container container = containerDsl.buildContainer();

        // then
        assertThat(container.getContainerName()).isEqualTo("fedora_10");

    }

    @Test
    public void should_create_container_with_link() {

        // given
        ContainerDsl containerDsl = new ContainerDsl("fedora")
                                            .withLink("mylink");

        // when
        final Container container = containerDsl.buildContainer();

        // then
        assertThat(container.getContainerName()).isEqualTo("fedora");
        assertThat(container.getCubeContainer().getLinks())
            .hasSize(1)
            .containsExactlyInAnyOrder(Link.valueOf("mylink"));
    }

}
