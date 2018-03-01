package org.arquillian.cube.impl;

import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;

import static org.assertj.core.api.Assertions.assertThat;

public class EnricherExpressionResolverTest {

    @Rule
    public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

    @Test
    public void should_resolve_property_from_config_map() {
        // given
        final EnricherExpressionResolver resolver =
            new EnricherExpressionResolver(ConfigurationParameters.from("app.name", "myapp"));

        // when
        final String resolvedString = resolver.resolve("${app.name}");

        // then
        assertThat(resolvedString).isEqualTo("myapp");
    }

    @Test
    public void should_overwrite_system_property_if_property_present_in_config_map() {
        // given
        System.setProperty("app.name", "systemProperty");

        final EnricherExpressionResolver resolver =
            new EnricherExpressionResolver(ConfigurationParameters.from("app.name", "myapp"));

        // when
        final String resolvedString = resolver.resolve("${app.name}");

        // then
        assertThat(resolvedString).isEqualTo("systemProperty");
    }

    @Test
    public void should_not_resolve_property_if_not_set_in_config_and_system_property() {
        // given
        final EnricherExpressionResolver resolver =
            new EnricherExpressionResolver(ConfigurationParameters.from("foo", "bar"));

        // when
        final String resolvedString = resolver.resolve("${app.name}");

        // then
        assertThat(resolvedString).isEqualTo("${app.name}");
    }
}
