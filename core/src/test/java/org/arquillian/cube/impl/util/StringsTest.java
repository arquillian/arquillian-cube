package org.arquillian.cube.impl.util;

import java.util.List;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StringsTest {

    @Test
    public void should_split_test_separated_by_commas() {
        // given
        String content = "file://${basedir}/elasticsearch.yml,file://${basedir}/configmap.yml";

        // when
        final List<String> strings = Strings.splitAndTrimAsList(content, "\\s*,\\s*");

        //then
        assertThat(strings)
            .containsExactly("file://${basedir}/elasticsearch.yml", "file://${basedir}/configmap.yml");
    }

}
