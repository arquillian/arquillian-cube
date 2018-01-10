package org.arquillian.cube.kubernetes.impl;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;


public class DefaultConfigurationTest {

    @Test
    public void shouldParseNullMap() throws Exception {
        Map<String, String> result = DefaultConfiguration.parseMap(null);
        assertThat(result).hasSize(0);
    }

    @Test
    public void shouldParseEmptyMap() throws Exception {
        Map<String, String> result = DefaultConfiguration.parseMap("");
        assertThat(result).hasSize(0);
    }

    @Test //We just want to know it won't barf
    public void shouldParseJustAKey() throws Exception {
        String s = "KEY1";

        Map<String, String> result = DefaultConfiguration.parseMap(s);
        assertThat(result).hasSize(1);
    }

    @Test
    public void shouldParseNormalMap() throws Exception {
       String s = "KEY1=VALUE1\n" +
        "KEY_TWO=VALUE2\n" +
        "KEY3=VALUE3\n" +
        "key4=VALUE4\n" +
        "KEY_5=VALUE5";

        Map<String, String> result = DefaultConfiguration.parseMap(s);
        assertThat(result).hasSize(5).containsKeys("KEY3", "key4", "KEY_5");
    }

}

