package org.arquillian.cube.docker.impl.client.config;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class ImageParserTest {

    @Test
    public void shouldParseSimpleImages() {
        final Image image = Image.valueOf("my_image");
        assertThat(image.getName(), is("my_image"));
        assertThat(image.getTag(), is(nullValue()));
    }

    @Test
    public void shouldParseSimpleImagesWithTag() {
        final Image image = Image.valueOf("my_image:tag");
        assertThat(image.getName(), is("my_image"));
        assertThat(image.getTag(), is("tag"));
    }

    @Test
    public void shouldParseOrganizationImages() {
        final Image image = Image.valueOf("organization/my_image");
        assertThat(image.getName(), is("organization/my_image"));
        assertThat(image.getTag(), is(nullValue()));
    }

    @Test
    public void shouldParseOrganizationalImagesWithTag() {
        final Image image = Image.valueOf("organization/my_image:tag");
        assertThat(image.getName(), is("organization/my_image"));
        assertThat(image.getTag(), is("tag"));
    }

    @Test
    public void shouldParseRepositoryImages() {
        final Image image = Image.valueOf("localhost:5000/organization/my_image");
        assertThat(image.getName(), is("localhost:5000/organization/my_image"));
        assertThat(image.getTag(), is(nullValue()));
    }

    @Test
    public void shouldParseRepositoryImagesAndSeveralOrganizationLevels() {
        final Image image = Image.valueOf("localhost:5000/organization/organization2/my_image");
        assertThat(image.getName(), is("localhost:5000/organization/organization2/my_image"));
        assertThat(image.getTag(), is(nullValue()));
    }

    @Test
    public void shouldParseRepositoryImagesWithTag() {
        final Image image = Image.valueOf("localhost:5000/organization/my_image:tag");
        assertThat(image.getName(), is("localhost:5000/organization/my_image"));
        assertThat(image.getTag(), is("tag"));
    }

    @Test
    public void shouldParseRepositoryImagesWithTagAndSeveralOrganizationLevels() {
        final Image image = Image.valueOf("localhost:5000/organization/organization2/my_image:tag");
        assertThat(image.getName(), is("localhost:5000/organization/organization2/my_image"));
        assertThat(image.getTag(), is("tag"));
    }
}
