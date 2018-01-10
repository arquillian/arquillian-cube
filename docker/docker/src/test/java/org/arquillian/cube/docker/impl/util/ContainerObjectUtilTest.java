package org.arquillian.cube.docker.impl.util;

import java.util.List;
import org.arquillian.cube.containerobject.Cube;
import org.arquillian.cube.containerobject.Environment;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

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
        final String value =
            ContainerObjectUtil.getTopCubeAttribute(DefaultAnnotationWithExtension.class, "value", Cube.class, "");
        assertThat(value, is("secondValue"));
    }

    @Test
    public void shouldReturnArrays() throws NoSuchMethodException {
        final String[] ports = ContainerObjectUtil.
            getTopCubeAttribute(FirstClassWithArray.class, "portBinding", Cube.class, new String[] {});
        assertThat(ports[0], is("2222->22/tcp"));
    }

    @Test
    public void shouldReturnAnnotationsFromRootObject() {
        final List<Environment> environments =
            (List<Environment>) ContainerObjectUtil.getAllAnnotations(SecondEnvironmentAnnotation.class,
                Environment.class);
        assertThat(environments.size(), is(1));
    }

    @Test
    public void shouldReturnAggregationAnnotationsOfAllObjectHierarchy() {
        final List<Environment> environments =
            (List<Environment>) ContainerObjectUtil.getAllAnnotations(FirstEnvironmentAnnotation.class,
                Environment.class);
        assertThat(environments.size(), is(2));
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

    @Environment(key = "A", value = "B")
    public static class SecondEnvironmentAnnotation {
    }

    @Environment(key = "C", value = "D")
    public static class FirstEnvironmentAnnotation extends SecondEnvironmentAnnotation {
    }
}
