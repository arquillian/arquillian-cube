package org.arquillian.cube.docker.impl.util;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.arquillian.cube.containerobject.Cube;
import org.junit.Test;

public class ContainerObjectUtilTest {

    @Test
    public void shouldReturnFirstValue() throws NoSuchMethodException {
        final String value = ContainerObjectUtil.getTopCubeAttribute(SecondClassAnnotated.class, "value", Cube.class, "");
        assertThat(value, is("secondValue"));
    }

    @Test
    public void shouldReturnTopValue() throws NoSuchMethodException {
        final String value = ContainerObjectUtil.getTopCubeAttribute(FirstClassAnnotated.class, "value", Cube.class, "");
        assertThat(value, is("firstValue"));
    }

    @Test
    public void shouldReturnNullIfNoClassAnnotated() throws NoSuchMethodException {
        final String value = ContainerObjectUtil.getTopCubeAttribute(EmptyClassAnnotation.class, "value", Cube.class, "");
        assertThat(value, is(nullValue()));
    }

    @Test
    public void shouldReturnParentValueIfClassNotAnnotated() throws NoSuchMethodException {
        final String value = ContainerObjectUtil.getTopCubeAttribute(ParentWithAnnotation.class, "value", Cube.class, "");
        assertThat(value, is("secondValue"));
    }

    @Test
    public void shouldReturnDefaultValueInCaseOfDefaults() throws NoSuchMethodException {
        final String value = ContainerObjectUtil.getTopCubeAttribute(DefaultAnnotation.class, "value", Cube.class, "");
        assertThat(value, is(""));
    }

    @Test
    public void shouldReturnParentValueIfCurrentIsDefault() throws NoSuchMethodException {
        final String value = ContainerObjectUtil.getTopCubeAttribute(DefaultAnnotationWithExtension.class, "value", Cube.class, "");
        assertThat(value, is("secondValue"));
    }

    @Test
    public void shouldReturnArrays() throws NoSuchMethodException {
        final String[] ports = ContainerObjectUtil.
                getTopCubeAttribute(FirstClassWithArray.class, "portBinding", Cube.class, new String[] {});
        assertThat(ports[0], is("2222->22/tcp"));
    }

    @Cube("secondValue")
    private static class SecondClassAnnotated {
    }

    @Cube("firstValue")
    private static class FirstClassAnnotated extends SecondClassAnnotated {
    }

    private static class EmptyClassAnnotation {
    }

    private static class ParentWithAnnotation extends SecondClassAnnotated {
    }

    @Cube
    private static class DefaultAnnotation {
    }

    @Cube
    private static class DefaultAnnotationWithExtension extends SecondClassAnnotated {
    }

    @Cube(portBinding = "2222->22/tcp")
    private static class FirstClassWithArray {
    }
}
