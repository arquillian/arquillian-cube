package org.arquillian.cube.docker.impl.util;

import org.apache.commons.lang.text.StrLookup;
import org.apache.commons.lang.text.StrSubstitutor;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class IOUtilTest {

    @Test
    public void shouldMergeToEmptyMap() {
        Map<String, Object> original = new HashMap<>();
        
        Map<String, Object> element = new HashMap<>();
        element.put("a", "b");
        Map<String, Object> element2 = new HashMap<>();
        element2.put("c", "d");
        element.put("e", element2);
        
        IOUtil.deepMerge(original, element);
        assertThat(original.containsKey("a"), is(true));
        assertThat(original.containsKey("e"), is(true));
        assertThat((String) original.get("a"), is("b"));
        Map<String, Object> innerElement = (Map<String, Object>) original.get("e");
        assertThat(innerElement.containsKey("c"), is(true));
        assertThat((String)innerElement.get("c"), is("d"));
    }



    @Test
    public void shouldAddSimpleAndComplexElements() {
        Map<String, Object> original = new HashMap<>();
        original.put("z", "y");

        Map<String, Object> element = new HashMap<>();
        element.put("a", "b");
        Map<String, Object> element2 = new HashMap<>();
        element2.put("c", "d");
        element.put("e", element2);

        IOUtil.deepMerge(original, element);
        assertThat(original.containsKey("a"), is(true));
        assertThat(original.containsKey("e"), is(true));
        assertThat(original.containsKey("z"), is(true));
        assertThat((String) original.get("a"), is("b"));
        assertThat((String) original.get("z"), is("y"));
        Map<String, Object> innerElement = (Map<String, Object>) original.get("e");
        assertThat(innerElement.containsKey("c"), is(true));
        assertThat((String)innerElement.get("c"), is("d"));
    }

    @Test
    public void shouldOverrideSimpleAndComplexElements() {
        Map<String, Object> original = new HashMap<>();
        Map<String, Object> org = new HashMap<>();
        org.put("a", "1");
        Map<String, Object> org2 = new HashMap<>();
        org2.put("c", "2");
        org.put("e", org2);


        Map<String, Object> element = new HashMap<>();
        element.put("a", "b");
        Map<String, Object> element2 = new HashMap<>();
        element2.put("c", "d");
        element.put("e", element2);

        IOUtil.deepMerge(original, element);
        assertThat(original.containsKey("a"), is(true));
        assertThat(original.containsKey("e"), is(true));
        assertThat((String) original.get("a"), is("b"));
        Map<String, Object> innerElement = (Map<String, Object>) original.get("e");
        assertThat(innerElement.containsKey("c"), is(true));
        assertThat((String)innerElement.get("c"), is("d"));
    }

}
