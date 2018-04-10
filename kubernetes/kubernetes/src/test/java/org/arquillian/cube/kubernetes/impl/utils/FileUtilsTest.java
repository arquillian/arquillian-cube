package org.arquillian.cube.kubernetes.impl.utils;

import java.nio.file.Paths;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import static org.arquillian.cube.kubernetes.impl.utils.FileUtils.resourceName;
import static org.arquillian.cube.kubernetes.impl.utils.FileUtils.resourceSuffix;

public class FileUtilsTest {

    @Test
    public void should_get_file_name_without_extension() {
        final String resource = resourceName(Paths.get("foo/bar/hello.yml"));

        Assertions.assertThat(resource).isEqualTo("hello");
    }

    @Test
    public void should_get_file_extension() {
        final String resource = resourceSuffix(Paths.get("foo/bar/hello.yml"));

        Assertions.assertThat(resource).isEqualTo(".yml");
    }
}
